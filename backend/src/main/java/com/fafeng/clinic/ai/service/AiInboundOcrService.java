package com.fafeng.clinic.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fafeng.clinic.ai.client.OcrClient;
import com.fafeng.clinic.ai.client.AiChatClient;
import com.fafeng.clinic.ai.config.ClinicAiProperties;
import com.fafeng.clinic.ai.config.ClinicOcrProperties;
import com.fafeng.clinic.ai.config.ExternalServiceConfigService;
import com.fafeng.clinic.ai.entity.AiDraft;
import com.fafeng.clinic.ai.model.InboundDraftLine;
import com.fafeng.clinic.ai.model.InboundDraftPayload;
import com.fafeng.clinic.ai.provider.AiProvider;
import com.fafeng.clinic.ai.vo.AiDraftVO;
import com.fafeng.clinic.ai.vo.OcrStatusVO;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.Desensitizer;
import com.fafeng.clinic.common.ErrorCode;
import com.fafeng.clinic.medicine.service.MedicineService;
import com.fafeng.clinic.medicine.vo.MedicineListItemVO;
import com.fafeng.clinic.medicine.vo.PageVO;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AiInboundOcrService {

    private static final DateTimeFormatter FILE_TS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final OcrClient ocrClient;
    private final AiChatClient aiChatClient;
    private final AiProvider activeAiProvider;
    private final ClinicAiProperties aiProperties;
    private final ExternalServiceConfigService externalServiceConfigService;
    private final ClinicOcrProperties ocrProperties;
    private final AiDraftService aiDraftService;
    private final MedicineService medicineService;
    private final ObjectMapper objectMapper;

    public AiInboundOcrService(OcrClient ocrClient,
                               AiChatClient aiChatClient,
                               AiProvider activeAiProvider,
                               ClinicAiProperties aiProperties,
                               ExternalServiceConfigService externalServiceConfigService,
                               ClinicOcrProperties ocrProperties,
                               AiDraftService aiDraftService,
                               MedicineService medicineService,
                               ObjectMapper objectMapper) {
        this.ocrClient = ocrClient;
        this.aiChatClient = aiChatClient;
        this.activeAiProvider = activeAiProvider;
        this.aiProperties = aiProperties;
        this.externalServiceConfigService = externalServiceConfigService;
        this.ocrProperties = ocrProperties;
        this.aiDraftService = aiDraftService;
        this.medicineService = medicineService;
        this.objectMapper = objectMapper;
    }

    public OcrStatusVO getStatus() {
        return new OcrStatusVO(
                ocrClient.isConfigured(),
                ocrClient.isConfigured(),
                externalServiceConfigService.getOcrMode(),
                externalServiceConfigService.getOcrVisionModel());
    }

    public AiDraftVO recognizeInbound(MultipartFile file) {
        if (!ocrClient.isConfigured()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "OCR 服务未配置");
        }
        if (!externalServiceConfigService.isChatEnabled() || !activeAiProvider.isAvailable()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 整理服务不可用，请检查 AI 配置");
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传图片");
        }

        byte[] bytes = readBytes(file);
        String filename = file.getOriginalFilename() == null ? "inbound.jpg" : file.getOriginalFilename();
        String imagePath = saveImage(bytes, filename);
        String ocrText = ocrClient.recognize(bytes, filename, file.getContentType());
        String desensitized = Desensitizer.desensitizeInboundDocument(ocrText);
        String structured = aiChatClient.chatCompletion(
                aiProperties.getInboundStructurePrompt(), desensitized, true);

        InboundDraftPayload payload = parseInboundPayload(structured);
        payload.setImagePath(imagePath);
        payload.setOcrText(ocrText);
        enrichMedicineMatches(payload);

        try {
            return aiDraftService.createStructuredDraft(AiDraft.TYPE_INBOUND, objectMapper.writeValueAsString(payload));
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "入库草稿保存失败");
        }
    }

    private InboundDraftPayload parseInboundPayload(String structured) {
        JsonNode root = AiJsonParser.parseJsonContent(objectMapper, structured);
        InboundDraftPayload payload = new InboundDraftPayload();
        payload.setSupplier(root.path("supplier").asText(""));
        payload.setRemark(root.path("remark").asText(""));

        List<InboundDraftLine> lines = new ArrayList<>();
        JsonNode lineNodes = root.path("lines");
        if (lineNodes.isArray()) {
            for (JsonNode node : lineNodes) {
                InboundDraftLine line = new InboundDraftLine();
                line.setMedicineName(node.path("medicineName").asText(""));
                line.setSpecification(node.path("specification").asText(""));
                line.setQuantity(node.path("quantity").asText(""));
                line.setUnit(node.path("unit").asText(""));
                line.setBatchNo(node.path("batchNo").asText(""));
                line.setExpiryDate(node.path("expiryDate").asText(""));
                line.setPurchasePrice(node.path("purchasePrice").asText(""));
                lines.add(line);
            }
        }
        payload.setLines(lines);
        return payload;
    }

    private void enrichMedicineMatches(InboundDraftPayload payload) {
        for (InboundDraftLine line : payload.getLines()) {
            if (line.getMedicineName() == null || line.getMedicineName().isBlank()) {
                continue;
            }
            PageVO<MedicineListItemVO> page = medicineService.search(line.getMedicineName(), "ACTIVE", 1, 5);
            if (page.records().isEmpty()) {
                line.setMatchNote("未匹配到药品，请手动选择或先维护药品资料");
                continue;
            }
            MedicineListItemVO best = page.records().getFirst();
            line.setMedicineId(best.id());
            line.setMatchNote("已匹配：" + best.name());
            if (line.getUnit() == null || line.getUnit().isBlank()) {
                line.setUnit(best.baseUnit());
            }
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "读取图片失败");
        }
    }

    private String saveImage(byte[] bytes, String filename) {
        try {
            Path dir = Paths.get(ocrProperties.getUploadDir());
            Files.createDirectories(dir);
            String safeName = filename.replaceAll("[^a-zA-Z0-9._-]", "_");
            String stored = FILE_TS.format(OffsetDateTime.now()) + "_" + UUID.randomUUID().toString().substring(0, 8) + "_" + safeName;
            Path target = dir.resolve(stored);
            Files.write(target, bytes);
            return ocrProperties.getUploadDir() + "/" + stored;
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "保存图片失败");
        }
    }
}
