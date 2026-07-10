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
 * 四类外部服务开关与 Whisper/OCR URL；DB 优先，表空时 bootstrap 读 env。
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

    public ServiceSnapshot getSnapshot(String serviceCode) {
        return snapshot.getOrDefault(serviceCode, ServiceSnapshot.disabled());
    }

    public Map<String, ServiceSnapshot> allSnapshots() {
        return snapshot;
    }

    private ServiceSnapshot bootstrapChat() {
        return new ServiceSnapshot(ExternalService.CODE_CHAT, aiProperties.isEnabled(), "");
    }

    private ServiceSnapshot bootstrapEmbedding() {
        return new ServiceSnapshot(ExternalService.CODE_EMBEDDING, embeddingProperties.isEnabled(), "");
    }

    private ServiceSnapshot bootstrapWhisper() {
        String url = voiceProperties.getWhisperUrl() == null ? "" : voiceProperties.getWhisperUrl().trim();
        boolean enabled = !url.isBlank();
        return new ServiceSnapshot(ExternalService.CODE_WHISPER, enabled, url);
    }

    private ServiceSnapshot bootstrapOcr() {
        String url = ocrProperties.getOcrUrl() == null ? "" : ocrProperties.getOcrUrl().trim();
        boolean enabled = !url.isBlank();
        return new ServiceSnapshot(ExternalService.CODE_OCR, enabled, url);
    }

    public record ServiceSnapshot(String serviceCode, boolean enabled, String endpointUrl) {
        static ServiceSnapshot disabled() {
            return new ServiceSnapshot("", false, "");
        }

        static ServiceSnapshot fromDb(ExternalService row) {
            return new ServiceSnapshot(
                    row.getServiceCode(),
                    Boolean.TRUE.equals(row.getEnabled()),
                    row.getEndpointUrl() == null ? "" : row.getEndpointUrl());
        }
    }
}
