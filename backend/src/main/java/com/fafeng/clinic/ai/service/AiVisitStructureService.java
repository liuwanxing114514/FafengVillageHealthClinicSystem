package com.fafeng.clinic.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fafeng.clinic.ai.config.ClinicAiProperties;
import com.fafeng.clinic.ai.entity.AiDraft;
import com.fafeng.clinic.ai.model.VisitDraftPayload;
import com.fafeng.clinic.ai.provider.AiProvider;
import com.fafeng.clinic.ai.vo.AiDraftVO;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.Desensitizer;
import com.fafeng.clinic.common.ErrorCode;
import org.springframework.stereotype.Service;

@Service
public class AiVisitStructureService {

    private final AiDraftService aiDraftService;
    private final AiProvider activeAiProvider;
    private final ClinicAiProperties properties;
    private final ObjectMapper objectMapper;

    public AiVisitStructureService(AiDraftService aiDraftService,
                                   AiProvider activeAiProvider,
                                   ClinicAiProperties properties,
                                   ObjectMapper objectMapper) {
        this.aiDraftService = aiDraftService;
        this.activeAiProvider = activeAiProvider;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public AiDraftVO structureVisit(String text, Long patientId) {
        if (!properties.isEnabled()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 功能未启用");
        }
        if (!activeAiProvider.isAvailable()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 服务不可用，请检查配置");
        }
        String desensitized = Desensitizer.desensitizeText(text.trim());
        String response = activeAiProvider.chatCompletion(properties.getVisitStructurePrompt(), desensitized);

        JsonNode root = AiJsonParser.parseJsonContent(objectMapper, response);
        VisitDraftPayload payload = new VisitDraftPayload();
        payload.setPatientId(patientId);
        payload.setSourceText(text.trim());
        payload.setChiefComplaint(root.path("chiefComplaint").asText(""));
        payload.setPresentIllness(root.path("presentIllness").asText(""));
        payload.setPastHistory(root.path("pastHistory").asText(""));
        payload.setDiagnosis(root.path("diagnosis").asText(""));
        payload.setTreatment(root.path("treatment").asText(""));
        payload.setRemark(root.path("remark").asText(""));

        try {
            return aiDraftService.createStructuredDraft(AiDraft.TYPE_VISIT, objectMapper.writeValueAsString(payload));
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "草稿保存失败");
        }
    }
}
