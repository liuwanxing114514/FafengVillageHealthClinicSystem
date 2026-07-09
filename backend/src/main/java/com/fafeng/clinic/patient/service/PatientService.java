
package com.fafeng.clinic.patient.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fafeng.clinic.clinic.mapper.ClinicVisitMapper;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import com.fafeng.clinic.common.IdCardUtils;
import com.fafeng.clinic.medicine.vo.PageVO;
import com.fafeng.clinic.patient.dto.PatientSearchQuery;
import com.fafeng.clinic.patient.dto.SavePatientRequest;
import com.fafeng.clinic.patient.entity.Patient;
import com.fafeng.clinic.patient.mapper.PatientMapper;
import com.fafeng.clinic.patient.vo.PatientDetailVO;
import com.fafeng.clinic.patient.vo.PatientListItemVO;
import com.fafeng.clinic.system.service.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientMapper patientMapper;
    private final ClinicVisitMapper visitMapper;
    private final AuditLogService auditLogService;


    public PageVO<PatientListItemVO> search(PatientSearchQuery query, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        PatientSearchQuery normalized = normalizeQuery(query);

        var result = patientMapper.searchPage(
                new Page<>(safePage, safeSize),
                emptyToNull(normalized.keyword()),
                emptyToNull(normalized.name()),
                emptyToNull(normalized.phone()),
                emptyToNull(normalized.idCard()),
                emptyToNull(normalized.address()),
                emptyToNull(normalized.gender()),
                emptyToNull(normalized.remark()),
                normalized.ageMin(),
                normalized.ageMax());

        List<PatientListItemVO> records = result.getRecords().stream()
                .map(this::toListItem)
                .toList();
        return new PageVO<>(records, result.getTotal(), safePage, safeSize);
    }

    @Transactional
    public PatientDetailVO create(SavePatientRequest request) {
        Patient patient = new Patient();
        applyRequest(patient, request);
        patient.setStatus(Patient.STATUS_ACTIVE);
        patient.setCreatedAt(OffsetDateTime.now());
        patient.setUpdatedAt(OffsetDateTime.now());
        patientMapper.insert(patient);

        auditLogService.log("CREATE_PATIENT", "patient", patient.getId(),
                "{\"name\":\"" + escapeJson(patient.getName()) + "\"}");
        return getDetail(patient.getId());
    }

    public PatientDetailVO getDetail(Long id) {
        return toDetail(requirePatient(id));
    }

    @Transactional
    public PatientDetailVO update(Long id, SavePatientRequest request) {
        Patient patient = requirePatient(id);
        applyRequest(patient, request);
        patient.setUpdatedAt(OffsetDateTime.now());
        patientMapper.updateById(patient);

        auditLogService.log("UPDATE_PATIENT", "patient", patient.getId(),
                "{\"name\":\"" + escapeJson(patient.getName()) + "\"}");
        return getDetail(id);
    }

    @Transactional
    public void delete(Long id) {
        Patient patient = requirePatient(id);
        patient.setStatus(Patient.STATUS_DELETED);
        patient.setUpdatedAt(OffsetDateTime.now());
        patientMapper.updateById(patient);

        auditLogService.log("DELETE_PATIENT", "patient", patient.getId(),
                "{\"name\":\"" + escapeJson(patient.getName()) + "\"}");
    }

    public Patient requirePatient(Long id) {
        Patient patient = patientMapper.selectById(id);
        if (patient == null || !Patient.STATUS_ACTIVE.equals(patient.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "患者不存在");
        }
        return patient;
    }

    private void applyRequest(Patient patient, SavePatientRequest request) {
        validateGender(request.gender());
        String idCard = normalizeIdCard(request.idCard());
        if (idCard != null && !IdCardUtils.isValid(idCard)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "身份证号格式不正确");
        }
        ensureUniqueIdCard(idCard, patient.getId());

        LocalDate birthDate = request.birthDate();
        Integer age = request.age();
        boolean ageManual;

        if (idCard != null) {
            birthDate = IdCardUtils.parseBirthDate(idCard);
            age = IdCardUtils.calculateAge(birthDate, LocalDate.now());
            ageManual = false;
        } else if (birthDate != null) {
            age = IdCardUtils.calculateAge(birthDate, LocalDate.now());
            ageManual = false;
        } else {
            if (age == null) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写身份证号、出生日期或年龄");
            }
            ageManual = true;
        }

        patient.setName(request.name().trim());
        patient.setGender(request.gender().trim().toUpperCase());
        patient.setIdCard(idCard);
        patient.setBirthDate(birthDate);
        patient.setAge(age);
        patient.setAgeManual(ageManual);
        patient.setPhone(trimToEmpty(request.phone()));
        patient.setAddress(trimToEmpty(request.address()));
        patient.setRemark(trimToNull(request.remark()));
    }

    private void validateGender(String gender) {
        String normalized = gender == null ? "" : gender.trim().toUpperCase();
        if (!Patient.GENDER_MALE.equals(normalized)
                && !Patient.GENDER_FEMALE.equals(normalized)
                && !Patient.GENDER_UNKNOWN.equals(normalized)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "性别值无效");
        }
    }

    private void ensureUniqueIdCard(String idCard, Long excludeId) {
        if (idCard == null) {
            return;
        }
        LambdaQueryWrapper<Patient> wrapper = new LambdaQueryWrapper<Patient>()
                .eq(Patient::getIdCard, idCard)
                .ne(Patient::getStatus, Patient.STATUS_DELETED);
        if (excludeId != null) {
            wrapper.ne(Patient::getId, excludeId);
        }
        if (patientMapper.selectCount(wrapper) > 0) {
            throw new BusinessException(ErrorCode.CONFLICT, "身份证号已被其他患者使用");
        }
    }

    private String normalizeIdCard(String idCard) {
        if (idCard == null) {
            return null;
        }
        String trimmed = idCard.trim().toUpperCase();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private PatientListItemVO toListItem(Patient patient) {
        return new PatientListItemVO(
                patient.getId(),
                patient.getName(),
                patient.getGender(),
                patient.getIdCard(),
                patient.getBirthDate(),
                patient.getAge(),
                patient.getAgeManual(),
                patient.getPhone(),
                patient.getAddress(),
                patient.getUpdatedAt());
    }

    private PatientDetailVO toDetail(Patient patient) {
        return new PatientDetailVO(
                patient.getId(),
                patient.getName(),
                patient.getGender(),
                patient.getIdCard(),
                patient.getBirthDate(),
                patient.getAge(),
                patient.getAgeManual(),
                patient.getPhone(),
                patient.getAddress(),
                patient.getRemark(),
                resolveTotalArrears(patient.getId()),
                patient.getStatus(),
                patient.getCreatedAt(),
                patient.getUpdatedAt());
    }

    private BigDecimal resolveTotalArrears(Long patientId) {
        BigDecimal sum = visitMapper.sumArrearsByPatientId(patientId);
        if (sum == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return sum.setScale(2, RoundingMode.HALF_UP);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String escapeJson(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private PatientSearchQuery normalizeQuery(PatientSearchQuery query) {
        if (query == null) {
            return new PatientSearchQuery(null, null, null, null, null, null, null, null, null);
        }
        String gender = emptyToNull(query.gender());
        if (gender != null) {
            gender = gender.toUpperCase();
        }
        return new PatientSearchQuery(
                emptyToNull(query.keyword()),
                emptyToNull(query.name()),
                emptyToNull(query.phone()),
                emptyToNull(query.idCard()),
                emptyToNull(query.address()),
                gender,
                emptyToNull(query.remark()),
                query.ageMin(),
                query.ageMax());
    }

    private String emptyToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
