
package com.fafeng.clinic.clinic.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fafeng.clinic.clinic.dto.SaveVisitRequest;
import com.fafeng.clinic.clinic.dto.VisitSearchQuery;
import com.fafeng.clinic.clinic.entity.ClinicVisit;
import com.fafeng.clinic.clinic.mapper.ClinicVisitMapper;
import com.fafeng.clinic.clinic.vo.VisitDetailVO;
import com.fafeng.clinic.clinic.vo.VisitFeeSummaryVO;
import com.fafeng.clinic.clinic.vo.VisitListItemVO;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import com.fafeng.clinic.medicine.vo.PageVO;
import com.fafeng.clinic.patient.entity.Patient;
import com.fafeng.clinic.patient.service.PatientService;
import com.fafeng.clinic.ai.service.QuickPhraseService;
import com.fafeng.clinic.system.service.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import lombok.RequiredArgsConstructor;

/**
 * 门诊病历 CRUD、搜索与就诊收费（应收/实收/欠款）。
 */
@Service
@RequiredArgsConstructor
public class VisitService {

    private final ClinicVisitMapper visitMapper;
    private final PatientService patientService;
    private final AuditLogService auditLogService;
    private final QuickPhraseService quickPhraseService;
    private final VisitFeeService visitFeeService;


    public PageVO<VisitListItemVO> search(VisitSearchQuery query, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 100);
        String keyword = query == null || query.keyword() == null ? "" : query.keyword().trim();
        Boolean arrearsOnly = query != null && Boolean.TRUE.equals(query.arrearsOnly());
        OffsetDateTime dateFrom = toStartOfDay(query == null ? null : query.dateFrom());
        OffsetDateTime dateTo = toStartOfNextDay(query == null ? null : query.dateTo());

        var result = visitMapper.searchPage(
                new Page<>(safePage, safeSize),
                keyword.isEmpty() ? null : keyword,
                dateFrom,
                dateTo,
                arrearsOnly ? Boolean.TRUE : null);

        List<VisitListItemVO> records = result.getRecords().stream()
                .map(this::toListItem)
                .toList();
        return new PageVO<>(records, result.getTotal(), safePage, safeSize);
    }

    public List<VisitListItemVO> listByPatient(Long patientId) {
        patientService.requirePatient(patientId);
        return visitMapper.listByPatientId(patientId).stream()
                .map(this::toListItem)
                .toList();
    }

    public BigDecimal getPatientTotalArrears(Long patientId) {
        patientService.requirePatient(patientId);
        BigDecimal sum = visitMapper.sumArrearsByPatientId(patientId);
        return sum == null ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
                : sum.setScale(2, RoundingMode.HALF_UP);
    }

    public VisitFeeSummaryVO getFeeSummary(Long visitId) {
        requireVisit(visitId);
        return visitFeeService.summarizeForVisit(visitId);
    }

    @Transactional
    public VisitDetailVO create(SaveVisitRequest request) {
        Patient patient = patientService.requirePatient(request.patientId());
        ClinicVisit visit = new ClinicVisit();
        visit.setPatientId(patient.getId());
        applyRequest(visit, request);
        visit.setStatus(ClinicVisit.STATUS_ACTIVE);
        visit.setCreatedAt(OffsetDateTime.now());
        visit.setUpdatedAt(OffsetDateTime.now());
        visitMapper.insert(visit);

        quickPhraseService.recordFromVisit(visit);
        auditLogService.log("CREATE_VISIT", "clinic_visit", visit.getId(),
                "{\"patientId\":" + patient.getId() + "}");
        return getDetail(visit.getId());
    }

    public VisitDetailVO getDetail(Long id) {
        return toDetail(requireVisit(id));
    }

    @Transactional
    public VisitDetailVO update(Long id, SaveVisitRequest request) {
        ClinicVisit visit = requireVisit(id);
        if (!visit.getPatientId().equals(request.patientId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能更换病历所属患者");
        }
        patientService.requirePatient(request.patientId());
        applyRequest(visit, request);
        visit.setUpdatedAt(OffsetDateTime.now());
        visitMapper.updateById(visit);

        quickPhraseService.recordFromVisit(visit);
        auditLogService.log("UPDATE_VISIT", "clinic_visit", visit.getId(),
                "{\"patientId\":" + visit.getPatientId() + "}");
        return getDetail(id);
    }

    @Transactional
    public void delete(Long id) {
        ClinicVisit visit = requireVisit(id);
        visit.setStatus(ClinicVisit.STATUS_VOID);
        visit.setUpdatedAt(OffsetDateTime.now());
        visitMapper.updateById(visit);

        auditLogService.log("DELETE_VISIT", "clinic_visit", visit.getId(),
                "{\"patientId\":" + visit.getPatientId() + "}");
    }

    ClinicVisit requireVisit(Long id) {
        ClinicVisit visit = visitMapper.selectById(id);
        if (visit == null || !ClinicVisit.STATUS_ACTIVE.equals(visit.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "病历不存在");
        }
        return visit;
    }

    private void applyRequest(ClinicVisit visit, SaveVisitRequest request) {
        visit.setVisitTime(request.visitTime() == null ? OffsetDateTime.now() : request.visitTime());
        visit.setChiefComplaint(trimToNull(request.chiefComplaint()));
        visit.setPresentIllness(trimToNull(request.presentIllness()));
        visit.setPastHistory(trimToNull(request.pastHistory()));
        visit.setTemperature(request.temperature());
        visit.setBloodPressure(trimToNull(request.bloodPressure()));
        visit.setSpo2(request.spo2());
        visit.setEtco2(request.etco2());
        visit.setHeartRate(request.heartRate());
        visit.setPulse(trimToNull(request.pulse()));
        visit.setAllergyHistory(trimToNull(request.allergyHistory()));
        visit.setDiagnosis(trimToNull(request.diagnosis()));
        visit.setTreatment(trimToNull(request.treatment()));
        visit.setRemark(trimToNull(request.remark()));
        visit.setAmountDue(normalizeMoney(request.amountDue()));
        visit.setAmountPaid(normalizeMoney(request.amountPaid()));
    }

    private VisitListItemVO toListItem(ClinicVisit visit) {
        Patient patient = patientService.requirePatient(visit.getPatientId());
        BigDecimal due = normalizeMoney(visit.getAmountDue());
        BigDecimal paid = normalizeMoney(visit.getAmountPaid());
        return new VisitListItemVO(
                visit.getId(),
                visit.getPatientId(),
                patient.getName(),
                patient.getGender(),
                visit.getVisitTime(),
                visit.getChiefComplaint(),
                visit.getDiagnosis(),
                due,
                paid,
                balance(due, paid),
                visit.getStatus());
    }

    private VisitDetailVO toDetail(ClinicVisit visit) {
        Patient patient = patientService.requirePatient(visit.getPatientId());
        BigDecimal due = normalizeMoney(visit.getAmountDue());
        BigDecimal paid = normalizeMoney(visit.getAmountPaid());
        VisitFeeSummaryVO feeSummary = visitFeeService.summarizeForVisit(visit.getId());
        return new VisitDetailVO(
                visit.getId(),
                visit.getPatientId(),
                patient.getName(),
                visit.getVisitTime(),
                visit.getChiefComplaint(),
                visit.getPresentIllness(),
                visit.getPastHistory(),
                visit.getTemperature(),
                visit.getBloodPressure(),
                visit.getSpo2(),
                visit.getEtco2(),
                visit.getHeartRate(),
                visit.getPulse(),
                visit.getAllergyHistory(),
                visit.getDiagnosis(),
                visit.getTreatment(),
                visit.getRemark(),
                due,
                paid,
                balance(due, paid),
                feeSummary.suggestedAmountDue(),
                feeSummary.referencePurchaseCost(),
                getPatientTotalArrears(visit.getPatientId()),
                visit.getStatus(),
                visit.getCreatedAt(),
                visit.getUpdatedAt());
    }

    private BigDecimal balance(BigDecimal due, BigDecimal paid) {
        return due.subtract(paid).max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private OffsetDateTime toStartOfDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.atStartOfDay().atOffset(ZoneOffset.ofHours(8));
    }

    private OffsetDateTime toStartOfNextDay(LocalDate date) {
        if (date == null) {
            return null;
        }
        return date.plusDays(1).atStartOfDay().atOffset(ZoneOffset.ofHours(8));
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
