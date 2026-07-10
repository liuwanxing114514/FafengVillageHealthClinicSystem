package com.fafeng.clinic.ai.config;

import com.fafeng.clinic.ai.entity.ExternalService;
import com.fafeng.clinic.ai.mapper.ExternalServiceMapper;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 四类外部服务开关与 Whisper/OCR URL 的运行时快照。
 *
 * <p><b>优先级：</b>{@code external_service} 表有任意一行 → 只读 DB；表空 → 从
 * {@link ClinicAiProperties} / {@link ClinicEmbeddingProperties} 等 bootstrap（对应 .env）。
 * 设置页首次改开关时会写入 DB（见 {@link com.fafeng.clinic.ai.service.ExternalServiceService}），此后以 DB 为准。
 *
 * <p>业务层应用 {@link #isChatEnabled()} 等判断，而不是直接读 env。
 */
@Service
public class ExternalServiceConfigService {

    private final ExternalServiceMapper externalServiceMapper;
    private final ClinicAiProperties aiProperties;
    private final ClinicEmbeddingProperties embeddingProperties;
    private final ClinicVoiceProperties voiceProperties;
    private final ClinicOcrProperties ocrProperties;

    private volatile boolean dbBacked;
    private volatile Map<String, ServiceSnapshot> snapshot = Map.of();

    public ExternalServiceConfigService(ExternalServiceMapper externalServiceMapper,
                                        ClinicAiProperties aiProperties,
                                        ClinicEmbeddingProperties embeddingProperties,
                                        ClinicVoiceProperties voiceProperties,
                                        ClinicOcrProperties ocrProperties) {
        this.externalServiceMapper = externalServiceMapper;
        this.aiProperties = aiProperties;
        this.embeddingProperties = embeddingProperties;
        this.voiceProperties = voiceProperties;
        this.ocrProperties = ocrProperties;
    }

    @PostConstruct
    public void init() {
        refresh();
    }

    public synchronized void refresh() {
        long count = externalServiceMapper.selectCount(null);
        dbBacked = count > 0;
        if (dbBacked) {
            List<ExternalService> rows = externalServiceMapper.selectList(null);
            Map<String, ServiceSnapshot> map = new ConcurrentHashMap<>();
            for (ExternalService row : rows) {
                map.put(row.getServiceCode(), ServiceSnapshot.fromDb(row));
            }
            snapshot = Map.copyOf(map);
        } else {
            snapshot = Map.of(
                    ExternalService.CODE_CHAT, bootstrapChat(),
                    ExternalService.CODE_EMBEDDING, bootstrapEmbedding(),
                    ExternalService.CODE_WHISPER, bootstrapWhisper(),
                    ExternalService.CODE_OCR, bootstrapOcr());
        }
    }

    public boolean isDbBacked() {
        return dbBacked;
    }

    public boolean isChatEnabled() {
        return snapshot.getOrDefault(ExternalService.CODE_CHAT, ServiceSnapshot.disabled()).enabled();
    }

    public boolean isEmbeddingEnabled() {
        return snapshot.getOrDefault(ExternalService.CODE_EMBEDDING, ServiceSnapshot.disabled()).enabled();
    }

    public boolean isWhisperEnabled() {
        return snapshot.getOrDefault(ExternalService.CODE_WHISPER, ServiceSnapshot.disabled()).enabled();
    }

    public boolean isOcrEnabled() {
        return snapshot.getOrDefault(ExternalService.CODE_OCR, ServiceSnapshot.disabled()).enabled();
    }

    public String getWhisperUrl() {
        return snapshot.getOrDefault(ExternalService.CODE_WHISPER, ServiceSnapshot.disabled()).endpointUrl();
    }

    public String getOcrUrl() {
        return snapshot.getOrDefault(ExternalService.CODE_OCR, ServiceSnapshot.disabled()).endpointUrl();
    }

    public String getOcrMode() {
        ServiceSnapshot ocr = snapshot.getOrDefault(ExternalService.CODE_OCR, ServiceSnapshot.disabled());
        return OcrServiceOptions.parseMode(ocr.optionsJson());
    }

    public String getOcrVisionModel() {
        ServiceSnapshot ocr = snapshot.getOrDefault(ExternalService.CODE_OCR, ServiceSnapshot.disabled());
        return OcrServiceOptions.parseVisionModel(ocr.optionsJson());
    }

    public ServiceSnapshot getSnapshot(String serviceCode) {
        return snapshot.getOrDefault(serviceCode, ServiceSnapshot.disabled());
    }

    public Map<String, ServiceSnapshot> allSnapshots() {
        return snapshot;
    }

    private ServiceSnapshot bootstrapChat() {
        return new ServiceSnapshot(ExternalService.CODE_CHAT, aiProperties.isEnabled(), "", "{}");
    }

    private ServiceSnapshot bootstrapEmbedding() {
        return new ServiceSnapshot(ExternalService.CODE_EMBEDDING, embeddingProperties.isEnabled(), "", "{}");
    }

    private ServiceSnapshot bootstrapWhisper() {
        String url = voiceProperties.getWhisperUrl() == null ? "" : voiceProperties.getWhisperUrl().trim();
        boolean enabled = !url.isBlank();
        return new ServiceSnapshot(ExternalService.CODE_WHISPER, enabled, url, "{}");
    }

    private ServiceSnapshot bootstrapOcr() {
        String url = ocrProperties.getOcrUrl() == null ? "" : ocrProperties.getOcrUrl().trim();
        String mode = ocrProperties.resolveMode();
        String visionModel = ocrProperties.getVisionModel() == null || ocrProperties.getVisionModel().isBlank()
                ? OcrServiceOptions.DEFAULT_VISION_MODEL
                : ocrProperties.getVisionModel().trim();
        boolean enabled = OcrServiceOptions.MODE_LOCAL.equals(mode)
                ? !url.isBlank()
                : aiProperties.isEnabled();
        String optionsJson = OcrServiceOptions.toJson(mode, visionModel);
        return new ServiceSnapshot(ExternalService.CODE_OCR, enabled, url, optionsJson);
    }

    public record ServiceSnapshot(String serviceCode, boolean enabled, String endpointUrl, String optionsJson) {
        static ServiceSnapshot disabled() {
            return new ServiceSnapshot("", false, "", "{}");
        }

        static ServiceSnapshot fromDb(ExternalService row) {
            String options = row.getOptionsJson();
            if (options == null || options.isBlank()) {
                options = "{}";
            }
            return new ServiceSnapshot(
                    row.getServiceCode(),
                    Boolean.TRUE.equals(row.getEnabled()),
                    row.getEndpointUrl() == null ? "" : row.getEndpointUrl(),
                    options);
        }
    }
}
