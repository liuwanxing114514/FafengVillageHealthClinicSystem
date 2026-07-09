package com.fafeng.clinic.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fafeng.clinic.ai.config.ClinicAiProperties;
import com.fafeng.clinic.ai.dto.StructureVisitRequest;
import com.fafeng.clinic.ai.dto.UpdateAiDraftPayloadRequest;
import com.fafeng.clinic.ai.entity.AiDraft;
import com.fafeng.clinic.ai.mapper.AiDraftMapper;
import com.fafeng.clinic.ai.model.VisitDraftPayload;
import com.fafeng.clinic.ai.provider.AiProvider;
import com.fafeng.clinic.ai.provider.DeepSeekAiProvider;
import com.fafeng.clinic.ai.util.Desensitizer;
import com.fafeng.clinic.ai.vo.AiDraftVO;
import com.fafeng.clinic.clinic.dto.SaveVisitRequest;
import com.fafeng.clinic.clinic.entity.ClinicVisit;
import com.fafeng.clinic.clinic.mapper.ClinicVisitMapper;
import com.fafeng.clinic.clinic.service.VisitService;
import com.fafeng.clinic.clinic.vo.VisitDetailVO;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import com.fafeng.clinic.patient.entity.Patient;
import com.fafeng.clinic.patient.service.PatientService;
import com.fafeng.clinic.system.service.AuditLogService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class AiVisitStructureService {

    private static final String DEFAULT_VISIT_PROMPT = """
            你是农村卫生室病历整理助手。请把用户提供的自由文本或语音转写内容，整理为结构化病历字段。
            只输出 JSON 对象，不要输出 Markdown 或解释。字段如下：
            chiefComplaint, presentIllness, pastHistory, allergyHistory, diagnosis, treatment, remark
            缺失字段用空字符串。不要编造未提及的信息。""";

    private final AiDraftMapper draftMapper;
    private final ClinicAiProperties properties;
    private final AiProvider activeAiProvider;
    private final DeepSeekAiProvider deepSeekAiProvider;
    private final PatientService patientService;
    private final ClinicVisitMapper visitMapper;
    private final VisitService visitService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public AiVisitStructureService(AiDraftMapper draftMapper,
                                   ClinicAiProperties properties,
                                   AiProvider activeAiProvider,
                                   DeepSeekAiProvider deepSeekAiProvider,
                                   PatientService patientService,
                                   ClinicVisitMapper visitMapper,
                                   VisitService visitService,
                                   AuditLogService auditLogService,
                                   ObjectMapper objectMapper) {
        this.draftMapper = draftMapper;
        this.properties = properties;
        this.activeAiProvider = activeAiProvider;
        this.deepSeekAiProvider = deepSeekAiProvider;
        this.patientService = patientService;
        this.visitMapper = visitMapper;
        this.visitService = visitService;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AiDraftVO structureVisit(StructureVisitRequest request) {
        requireDeepSeekAvailable();
        String inputText = request.text().trim();
        validateVisitContext(request.patientId(), request.visitId());

        Desensitizer.PatientContext patientContext = loadPatientContext(request.patientId());
        String desensitized = Desensitizer.desensitizeText(inputText, patientContext);
        String json = deepSeekAiProvider.chatCompletion(resolveVisitPrompt(), desensitized);
        VisitDraftPayload structured = parseStructuredVisit(json, request, inputText);

        AiDraft draft = new AiDraft();
        draft.setDraftType(AiDraft.TYPE_VISIT);
        draft.setStatus(AiDraft.STATUS_PENDING);
        draft.setPayload(writePayload(structured));
        draft.setSource(activeAiProvider.name());
        draft.setCreatedAt(OffsetDateTime.now());
        draft.setUpdatedAt(OffsetDateTime.now());
        draftMapper.insert(draft);

        auditLogService.log("AI_STRUCTURE_VISIT", "ai_draft", draft.getId(),
                "{\"patientId\":" + (request.patientId() == null ? "null" : request.patientId()) + "}");
        return toVO(draft);
    }

    @Transactional
    public AiDraftVO updatePayload(Long id, UpdateAiDraftPayloadRequest request) {
        AiDraft draft = requirePendingDraft(id, AiDraft.TYPE_VISIT);
        VisitDraftPayload payload = readPayload(request.payload());
        if (payload.patientId() == null && payload.visitId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "草稿需关联患者或病历");
        }
        validateVisitContext(payload.patientId(), payload.visitId());
        draft.setPayload(writePayload(payload));
        draft.setUpdatedAt(OffsetDateTime.now());
        draftMapper.updateById(draft);
        return toVO(draft);
    }

    @Transactional
    public VisitDetailVO approveVisitDraft(Long id) {
        AiDraft draft = requirePendingDraft(id, AiDraft.TYPE_VISIT);
        VisitDraftPayload payload = readPayload(draft.getPayload());
        if (payload.patientId() == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "草稿缺少患者信息，无法写入病历");
        }
        patientService.requirePatient(payload.patientId());
        if (payload.visitId() != null) {
            ClinicVisit visit = visitMapper.selectById(payload.visitId());
            if (visit == null || !ClinicVisit.STATUS_ACTIVE.equals(visit.getStatus())) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "关联病历不存在");
            }
            if (!visit.getPatientId().equals(payload.patientId())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "草稿与病历所属患者不一致");
            }
        }

        SaveVisitRequest saveRequest = new SaveVisitRequest(
                payload.patientId(),
                null,
                payload.chiefComplaint(),
                payload.presentIllness(),
                payload.pastHistory(),
                null,
                null,
                null,
                null,
                null,
                null,
                payload.allergyHistory(),
                payload.diagnosis(),
                payload.treatment(),
                payload.remark()
        );

        VisitDetailVO visitDetail;
        if (payload.visitId() != null) {
            visitDetail = visitService.update(payload.visitId(), saveRequest);
        } else {
            visitDetail = visitService.create(saveRequest);
        }

        draft.setStatus(AiDraft.STATUS_APPROVED);
        draft.setUpdatedAt(OffsetDateTime.now());
        draftMapper.updateById(draft);

        auditLogService.log("AI_APPROVE_VISIT_DRAFT", "ai_draft", draft.getId(),
                "{\"visitId\":" + visitDetail.id() + "}");
        return visitDetail;
    }

    private void requireDeepSeekAvailable() {
        if (!properties.isEnabled()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 功能未启用");
        }
        if (!activeAiProvider.isAvailable() || !"deepseek".equals(activeAiProvider.name())) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
                    "DeepSeek 未配置或不可用，请检查 CLINIC_AI_ENABLED 与 DEEPSEEK_API_KEY");
        }
    }

    private void validateVisitContext(Long patientId, Long visitId) {
        if (visitId != null) {
            ClinicVisit visit = visitMapper.selectById(visitId);
            if (visit == null || !ClinicVisit.STATUS_ACTIVE.equals(visit.getStatus())) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "病历不存在");
            }
            if (patientId != null && !visit.getPatientId().equals(patientId)) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "患者与病历不匹配");
            }
        }
        if (patientId != null) {
            patientService.requirePatient(patientId);
        }
    }

    private Desensitizer.PatientContext loadPatientContext(Long patientId) {
        if (patientId == null) {
            return Desensitizer.PatientContext.empty();
        }
        Patient patient = patientService.requirePatient(patientId);
        return Desensitizer.PatientContext.of(
                patient.getName(),
                patient.getPhone(),
                patient.getAddress(),
                patient.getIdCard()
        );
    }

    private VisitDraftPayload parseStructuredVisit(String json, StructureVisitRequest request, String inputText) {
        try {
            JsonNode root = objectMapper.readTree(json);
            return new VisitDraftPayload(
                    request.patientId(),
                    request.visitId(),
                    inputText,
                    textField(root, "chiefComplaint"),
                    textField(root, "presentIllness"),
                    textField(root, "pastHistory"),
                    textField(root, "allergyHistory"),
                    textField(root, "diagnosis"),
                    textField(root, "treatment"),
                    textField(root, "remark")
            );
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 返回内容无法解析为病历字段");
        }
    }

    private String textField(JsonNode root, String field) {
        JsonNode node = root.path(field);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText("").trim();
        return value.isEmpty() ? null : value;
    }

    private VisitDraftPayload readPayload(String payloadJson) {
        try {
            VisitDraftPayload payload = objectMapper.readValue(payloadJson, VisitDraftPayload.class);
            if (payload.inputText() == null) {
                return new VisitDraftPayload(
                        payload.patientId(),
                        payload.visitId(),
                        "",
                        payload.chiefComplaint(),
                        payload.presentIllness(),
                        payload.pastHistory(),
                        payload.allergyHistory(),
                        payload.diagnosis(),
                        payload.treatment(),
                        payload.remark()
                );
            }
            return payload;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "草稿内容格式无效");
        }
    }

    private String writePayload(VisitDraftPayload payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "草稿序列化失败");
        }
    }

    private AiDraft requirePendingDraft(Long id, String draftType) {
        AiDraft draft = draftMapper.selectById(id);
        if (draft == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "AI 草稿不存在");
        }
        if (!draftType.equals(draft.getDraftType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "草稿类型不匹配");
        }
        if (!AiDraft.STATUS_PENDING.equals(draft.getStatus())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "仅待确认草稿可操作");
        }
        return draft;
    }

    private String resolveVisitPrompt() {
        String configured = properties.getVisitStructurePrompt();
        if (configured == null || configured.isBlank()) {
            return DEFAULT_VISIT_PROMPT;
        }
        return configured.trim();
    }

    private AiDraftVO toVO(AiDraft draft) {
        return new AiDraftVO(
                draft.getId(),
                draft.getDraftType(),
                draft.getStatus(),
                draft.getPayload(),
                draft.getSource(),
                draft.getCreatedAt(),
                draft.getUpdatedAt()
        );
    }
}
