package com.fafeng.clinic.ai.service;

import com.fafeng.clinic.ai.channel.ChannelRegistry;
import com.fafeng.clinic.ai.client.AiChatClient;
import com.fafeng.clinic.ai.client.ResilientEmbeddingModel;
import com.fafeng.clinic.ai.client.WhisperClient;
import com.fafeng.clinic.ai.client.OcrClient;
import com.fafeng.clinic.ai.config.ExternalServiceConfigService;
import com.fafeng.clinic.ai.dto.UpdateExternalServiceRequest;
import com.fafeng.clinic.ai.entity.ExternalService;
import com.fafeng.clinic.ai.mapper.ExternalServiceMapper;
import com.fafeng.clinic.ai.vo.ExternalServiceItemVO;
import com.fafeng.clinic.ai.vo.ExternalServicesOverviewVO;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import com.fafeng.clinic.system.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 设置页：四类外部服务（Chat/Embedding/Whisper/OCR）开关与 URL。
 *
 * <p>首次在设置页切换开关时，若 DB 尚无 {@code external_service} 行，会按当前 env 快照初始化四行，
 * 之后改开关只影响 DB，不再读 env。
 */
@Service
@RequiredArgsConstructor
public class ExternalServiceService {

    private static final Set<String> VALID_CODES = Set.of(
            ExternalService.CODE_CHAT,
            ExternalService.CODE_EMBEDDING,
            ExternalService.CODE_WHISPER,
            ExternalService.CODE_OCR);

    private final ExternalServiceMapper externalServiceMapper;
    private final ExternalServiceConfigService externalServiceConfigService;
    private final ChannelRegistry channelRegistry;
    private final AiChatClient aiChatClient;
    private final ResilientEmbeddingModel resilientEmbeddingModel;
    private final WhisperClient whisperClient;
    private final OcrClient ocrClient;
    private final AuditLogService auditLogService;

    public ExternalServicesOverviewVO getOverview() {
        Map<String, ExternalServiceItemVO> services = new LinkedHashMap<>();
        services.put(ExternalService.CODE_CHAT, buildItem(
                ExternalService.CODE_CHAT,
                externalServiceConfigService.isChatEnabled(),
                "",
                aiChatClient.isConfigured(),
                channelRegistry.chatChannelCount()));
        services.put(ExternalService.CODE_EMBEDDING, buildItem(
                ExternalService.CODE_EMBEDDING,
                externalServiceConfigService.isEmbeddingEnabled(),
                "",
                resilientEmbeddingModel.isConfigured(),
                channelRegistry.embeddingChannelCount()));
        services.put(ExternalService.CODE_WHISPER, buildItem(
                ExternalService.CODE_WHISPER,
                externalServiceConfigService.isWhisperEnabled(),
                externalServiceConfigService.getWhisperUrl(),
                whisperClient.isConfigured(),
                0));
        services.put(ExternalService.CODE_OCR, buildItem(
                ExternalService.CODE_OCR,
                externalServiceConfigService.isOcrEnabled(),
                externalServiceConfigService.getOcrUrl(),
                ocrClient.isConfigured(),
                0));
        return new ExternalServicesOverviewVO(
                services,
                externalServiceConfigService.isDbBacked(),
                channelRegistry.isDbChatBacked(),
                channelRegistry.isDbEmbeddingBacked());
    }

    @Transactional
    public ExternalServiceItemVO updateService(String serviceCode, UpdateExternalServiceRequest request) {
        validateCode(serviceCode);
        ensureDbInitializedIfNeeded();
        ExternalService row = externalServiceMapper.selectById(serviceCode);
        if (row == null) {
            row = new ExternalService();
            row.setServiceCode(serviceCode);
            row.setEnabled(false);
            row.setEndpointUrl("");
        }
        row.setEnabled(Boolean.TRUE.equals(request.enabled()));
        if (request.endpointUrl() != null) {
            row.setEndpointUrl(request.endpointUrl().trim());
        }
        row.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        if (externalServiceMapper.selectById(serviceCode) == null) {
            externalServiceMapper.insert(row);
        } else {
            externalServiceMapper.updateById(row);
        }
        externalServiceConfigService.refresh();
        auditLogService.log("UPDATE_EXTERNAL_SERVICE", "external_service", null,
                "{\"serviceCode\":\"" + serviceCode + "\",\"enabled\":" + row.getEnabled() + "}");
        return getOverview().services().get(serviceCode);
    }

    private void ensureDbInitializedIfNeeded() {
        if (!externalServiceConfigService.isDbBacked()) {
            OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
            insertIfMissing(ExternalService.CODE_CHAT, externalServiceConfigService.isChatEnabled(), "", now);
            insertIfMissing(ExternalService.CODE_EMBEDDING, externalServiceConfigService.isEmbeddingEnabled(), "", now);
            insertIfMissing(ExternalService.CODE_WHISPER, externalServiceConfigService.isWhisperEnabled(),
                    externalServiceConfigService.getWhisperUrl(), now);
            insertIfMissing(ExternalService.CODE_OCR, externalServiceConfigService.isOcrEnabled(),
                    externalServiceConfigService.getOcrUrl(), now);
        }
    }

    private void insertIfMissing(String code, boolean enabled, String endpointUrl, OffsetDateTime now) {
        if (externalServiceMapper.selectById(code) != null) {
            return;
        }
        ExternalService row = new ExternalService();
        row.setServiceCode(code);
        row.setEnabled(enabled);
        row.setEndpointUrl(endpointUrl == null ? "" : endpointUrl);
        row.setUpdatedAt(now);
        externalServiceMapper.insert(row);
    }

    private ExternalServiceItemVO buildItem(String code, boolean enabled, String endpointUrl,
                                            boolean configured, int channelCount) {
        return new ExternalServiceItemVO(code, enabled, endpointUrl == null ? "" : endpointUrl, configured, channelCount);
    }

    private void validateCode(String serviceCode) {
        if (serviceCode == null || !VALID_CODES.contains(serviceCode.trim())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "无效的外部服务代码");
        }
    }
}
