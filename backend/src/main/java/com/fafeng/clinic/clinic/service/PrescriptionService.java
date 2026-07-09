package com.fafeng.clinic.clinic.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafeng.clinic.clinic.dto.SavePrescriptionItemRequest;
import com.fafeng.clinic.clinic.dto.SavePrescriptionRequest;
import com.fafeng.clinic.clinic.entity.ClinicVisit;
import com.fafeng.clinic.clinic.entity.Prescription;
import com.fafeng.clinic.clinic.entity.PrescriptionItem;
import com.fafeng.clinic.clinic.mapper.ClinicVisitMapper;
import com.fafeng.clinic.clinic.mapper.PrescriptionItemMapper;
import com.fafeng.clinic.clinic.mapper.PrescriptionMapper;
import com.fafeng.clinic.clinic.vo.OutboundDraftItemVO;
import com.fafeng.clinic.clinic.vo.OutboundDraftVO;
import com.fafeng.clinic.clinic.vo.PrescriptionDetailVO;
import com.fafeng.clinic.clinic.vo.PrescriptionItemVO;
import com.fafeng.clinic.clinic.vo.PrescriptionPrintVO;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import com.fafeng.clinic.medicine.entity.Medicine;
import com.fafeng.clinic.medicine.service.MedicineService;
import com.fafeng.clinic.medicine.vo.MedicineDetailVO;
import com.fafeng.clinic.patient.entity.Patient;
import com.fafeng.clinic.patient.service.PatientService;
import com.fafeng.clinic.ai.service.QuickPhraseService;
import com.fafeng.clinic.system.service.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class PrescriptionService {

    private static final String CLINIC_NAME = "发凤村卫生室";
    private static final String PRESCRIPTION_TITLE = "发凤村卫生室处方签";

    private final PrescriptionMapper prescriptionMapper;
    private final PrescriptionItemMapper prescriptionItemMapper;
    private final ClinicVisitMapper visitMapper;
    private final PatientService patientService;
    private final MedicineService medicineService;
    private final AuditLogService auditLogService;
    private final QuickPhraseService quickPhraseService;
    private final PrescriptionPrintTemplateService printTemplateService;

    public PrescriptionService(PrescriptionMapper prescriptionMapper,
                               PrescriptionItemMapper prescriptionItemMapper,
                               ClinicVisitMapper visitMapper,
                               PatientService patientService,
                               MedicineService medicineService,
                               AuditLogService auditLogService,
                               QuickPhraseService quickPhraseService,
                               PrescriptionPrintTemplateService printTemplateService) {
        this.prescriptionMapper = prescriptionMapper;
        this.prescriptionItemMapper = prescriptionItemMapper;
        this.visitMapper = visitMapper;
        this.patientService = patientService;
        this.medicineService = medicineService;
        this.auditLogService = auditLogService;
        this.quickPhraseService = quickPhraseService;
        this.printTemplateService = printTemplateService;
    }

    @Transactional
    public PrescriptionDetailVO create(SavePrescriptionRequest request) {
        Patient patient = patientService.requirePatient(request.patientId());
        ClinicVisit visit = requireVisit(request.visitId());
        if (!visit.getPatientId().equals(patient.getId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "病历与患者不匹配");
        }

        Prescription prescription = new Prescription();
        prescription.setPatientId(patient.getId());
        prescription.setVisitId(visit.getId());
        prescription.setPrescriptionDate(
                request.prescriptionDate() == null ? LocalDate.now() : request.prescriptionDate());
        prescription.setDiagnosis(resolveDiagnosis(request.diagnosis(), visit.getDiagnosis()));
        prescription.setStatus(Prescription.STATUS_CONFIRMED);
        prescription.setCreatedAt(OffsetDateTime.now());
        prescription.setUpdatedAt(OffsetDateTime.now());
        prescriptionMapper.insert(prescription);

        saveItems(prescription.getId(), request.items());

        auditLogService.log("CREATE_PRESCRIPTION", "prescription", prescription.getId(),
                "{\"patientId\":" + patient.getId() + ",\"visitId\":" + visit.getId() + "}");
        return getDetail(prescription.getId());
    }

    public PrescriptionDetailVO getDetail(Long id) {
        return toDetail(requirePrescription(id));
    }

    @Transactional
    public PrescriptionDetailVO update(Long id, SavePrescriptionRequest request) {
        Prescription prescription = requirePrescription(id);
        Patient patient = patientService.requirePatient(request.patientId());
        ClinicVisit visit = requireVisit(request.visitId());
        if (!visit.getPatientId().equals(patient.getId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "病历与患者不匹配");
        }
        if (!prescription.getPatientId().equals(patient.getId())
                || !prescription.getVisitId().equals(visit.getId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不能更换处方关联的患者或病历");
        }

        prescription.setPrescriptionDate(
                request.prescriptionDate() == null ? prescription.getPrescriptionDate() : request.prescriptionDate());
        prescription.setDiagnosis(resolveDiagnosis(request.diagnosis(), visit.getDiagnosis()));
        prescription.setUpdatedAt(OffsetDateTime.now());
        prescriptionMapper.updateById(prescription);

        prescriptionItemMapper.deleteByPrescriptionId(id);
        saveItems(id, request.items());

        auditLogService.log("UPDATE_PRESCRIPTION", "prescription", prescription.getId(),
                "{\"patientId\":" + patient.getId() + "}");
        return getDetail(id);
    }

    @Transactional
    public void voidPrescription(Long id) {
        Prescription prescription = requirePrescription(id);
        prescription.setStatus(Prescription.STATUS_VOID);
        prescription.setUpdatedAt(OffsetDateTime.now());
        prescriptionMapper.updateById(prescription);

        auditLogService.log("VOID_PRESCRIPTION", "prescription", prescription.getId(),
                "{\"patientId\":" + prescription.getPatientId() + "}");
    }

    public PrescriptionPrintVO getPrintData(Long id) {
        Prescription prescription = requirePrescription(id);
        Patient patient = patientService.requirePatient(prescription.getPatientId());
        List<PrescriptionItemVO> items = listItemVOs(id);
        LocalDate date = prescription.getPrescriptionDate();

        return new PrescriptionPrintVO(
                CLINIC_NAME,
                PRESCRIPTION_TITLE,
                printTemplateService.getActiveTemplate(),
                printTemplateService.getTemplateConfigJson(),
                prescription.getVisitId(),
                "全科",
                patient.getName(),
                formatGender(patient.getGender()),
                patient.getAge(),
                patient.getAddress(),
                patient.getPhone(),
                prescription.getDiagnosis(),
                date,
                date == null ? null : date.getYear(),
                date == null ? null : date.getMonthValue(),
                date == null ? null : date.getDayOfMonth(),
                items,
                "医生签名：");
    }

    public OutboundDraftVO generateOutboundDraft(Long id) {
        Prescription prescription = requirePrescription(id);
        Patient patient = patientService.requirePatient(prescription.getPatientId());
        List<OutboundDraftItemVO> items = prescriptionItemMapper.listByPrescriptionId(id).stream()
                .map(item -> new OutboundDraftItemVO(
                        item.getMedicineId(),
                        item.getMedicineName(),
                        item.getSpecification(),
                        item.getQuantity(),
                        item.getUnit(),
                        item.getUsage()))
                .toList();

        return new OutboundDraftVO(
                prescription.getId(),
                patient.getId(),
                patient.getName(),
                prescription.getDiagnosis(),
                items);
    }

    public List<PrescriptionDetailVO> listByVisit(Long visitId) {
        requireVisit(visitId);
        return prescriptionMapper.selectList(new LambdaQueryWrapper<Prescription>()
                        .eq(Prescription::getVisitId, visitId)
                        .ne(Prescription::getStatus, Prescription.STATUS_VOID)
                        .orderByDesc(Prescription::getPrescriptionDate)
                        .orderByDesc(Prescription::getId))
                .stream()
                .map(this::toDetail)
                .toList();
    }

    private void saveItems(Long prescriptionId, List<SavePrescriptionItemRequest> items) {
        int sortOrder = 0;
        for (SavePrescriptionItemRequest itemRequest : items) {
            MedicineDetailVO medicine = medicineService.getDetail(itemRequest.medicineId());
            if (!Medicine.STATUS_ACTIVE.equals(medicine.status())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "药品已停用：" + medicine.name());
            }
            validateUnit(medicine, itemRequest.unit());

            PrescriptionItem item = new PrescriptionItem();
            item.setPrescriptionId(prescriptionId);
            item.setMedicineId(medicine.id());
            item.setDosageForm(medicine.dosageForm());
            item.setMedicineName(medicine.name());
            item.setSpecification(medicine.specification());
            item.setQuantity(itemRequest.quantity());
            item.setUnit(itemRequest.unit().trim());
            item.setUsage(trimToNull(itemRequest.usage()));
            item.setSortOrder(sortOrder++);
            item.setCreatedAt(OffsetDateTime.now());
            prescriptionItemMapper.insert(item);
            quickPhraseService.recordPrescriptionUsage(item.getUsage());
        }
    }

    private void validateUnit(MedicineDetailVO medicine, String unit) {
        String normalized = unit == null ? "" : unit.trim();
        if (normalized.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "药品单位不能为空");
        }
        boolean allowed = normalized.equals(medicine.baseUnit())
                || (medicine.packageUnit() != null && normalized.equals(medicine.packageUnit()))
                || medicine.conversions().stream().anyMatch(c ->
                normalized.equals(c.fromUnit()) || normalized.equals(c.toUnit()));
        if (!allowed) {
            throw new BusinessException(ErrorCode.BAD_REQUEST,
                    "单位「" + normalized + "」与药品「" + medicine.name() + "」不匹配");
        }
    }

    private Prescription requirePrescription(Long id) {
        Prescription prescription = prescriptionMapper.selectById(id);
        if (prescription == null || Prescription.STATUS_VOID.equals(prescription.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "处方不存在");
        }
        return prescription;
    }

    private ClinicVisit requireVisit(Long id) {
        ClinicVisit visit = visitMapper.selectById(id);
        if (visit == null || !ClinicVisit.STATUS_ACTIVE.equals(visit.getStatus())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "病历不存在");
        }
        return visit;
    }

    private String resolveDiagnosis(String requestDiagnosis, String visitDiagnosis) {
        String trimmed = trimToNull(requestDiagnosis);
        if (trimmed != null) {
            return trimmed;
        }
        return trimToNull(visitDiagnosis);
    }

    private List<PrescriptionItemVO> listItemVOs(Long prescriptionId) {
        return prescriptionItemMapper.listByPrescriptionId(prescriptionId).stream()
                .map(this::toItemVO)
                .toList();
    }

    private PrescriptionDetailVO toDetail(Prescription prescription) {
        Patient patient = patientService.requirePatient(prescription.getPatientId());
        return new PrescriptionDetailVO(
                prescription.getId(),
                prescription.getPatientId(),
                patient.getName(),
                prescription.getVisitId(),
                prescription.getPrescriptionDate(),
                prescription.getDiagnosis(),
                prescription.getStatus(),
                listItemVOs(prescription.getId()),
                prescription.getCreatedAt(),
                prescription.getUpdatedAt());
    }

    private PrescriptionItemVO toItemVO(PrescriptionItem item) {
        return new PrescriptionItemVO(
                item.getId(),
                item.getMedicineId(),
                item.getDosageForm(),
                item.getMedicineName(),
                item.getSpecification(),
                item.getQuantity(),
                item.getUnit(),
                item.getUsage(),
                item.getSortOrder());
    }

    private String formatGender(String gender) {
        if ("M".equalsIgnoreCase(gender)) {
            return "男";
        }
        if ("F".equalsIgnoreCase(gender)) {
            return "女";
        }
        return "未知";
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
