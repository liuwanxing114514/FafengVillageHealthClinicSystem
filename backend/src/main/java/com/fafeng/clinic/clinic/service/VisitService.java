package com.fafeng.clinic.clinic.service;

import com.fafeng.clinic.clinic.dto.SaveVisitRequest;
import com.fafeng.clinic.clinic.entity.ClinicVisit;
import com.fafeng.clinic.clinic.mapper.ClinicVisitMapper;
import com.fafeng.clinic.clinic.vo.VisitDetailVO;
import com.fafeng.clinic.clinic.vo.VisitListItemVO;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import com.fafeng.clinic.patient.entity.Patient;
import com.fafeng.clinic.patient.service.PatientService;
import com.fafeng.clinic.system.service.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class VisitService {

    private final ClinicVisitMapper visitMapper;
    private final PatientService patientService;
    private final AuditLogService auditLogService;

    public VisitService(ClinicVisitMapper visitMapper,
                        PatientService patientService,
                        AuditLogService auditLogService) {
        this.visitMapper = visitMapper;
        this.patientService = patientService;
        this.auditLogService = auditLogService;
    }

    public List<VisitListItemVO> listByPatient(Long patientId) {
        patientService.requirePatient(patientId);
        return visitMapper.listByPatientId(patientId).stream()
                .map(this::toListItem)
                .toList();
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

        auditLogService.log("UPDATE_VISIT", "clinic_visit", visit.getId(),
                "{\"patientId\":" + visit.getPatientId() + "}");
        return getDetail(id);
    }

    private ClinicVisit requireVisit(Long id) {
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
    }

    private VisitListItemVO toListItem(ClinicVisit visit) {
        return new VisitListItemVO(
                visit.getId(),
                visit.getPatientId(),
                visit.getVisitTime(),
                visit.getChiefComplaint(),
                visit.getDiagnosis(),
                visit.getStatus());
    }

    private VisitDetailVO toDetail(ClinicVisit visit) {
        Patient patient = patientService.requirePatient(visit.getPatientId());
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
                visit.getStatus(),
                visit.getCreatedAt(),
                visit.getUpdatedAt());
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
