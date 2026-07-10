package com.fafeng.clinic.ai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafeng.clinic.ai.channel.ChannelRegistry;
import com.fafeng.clinic.ai.channel.ChatChannelConfig;
import com.fafeng.clinic.ai.channel.ChatChannelFactory;
import com.fafeng.clinic.ai.channel.EmbeddingChannelConfig;
import com.fafeng.clinic.ai.channel.EmbeddingChannelFactory;
import com.fafeng.clinic.ai.channel.EnvBootstrapChannelSource;
import com.fafeng.clinic.ai.config.ClinicAiProperties;
import com.fafeng.clinic.ai.config.ClinicEmbeddingProperties;
import com.fafeng.clinic.ai.config.ExternalServiceConfigService;
import com.fafeng.clinic.ai.config.SecretEncryptor;
import com.fafeng.clinic.ai.dto.ReorderChannelsRequest;
import com.fafeng.clinic.ai.dto.SaveChatChannelRequest;
import com.fafeng.clinic.ai.dto.SaveEmbeddingChannelRequest;
import com.fafeng.clinic.ai.entity.AiChatChannel;
import com.fafeng.clinic.ai.entity.AiEmbeddingChannel;
import com.fafeng.clinic.ai.entity.ExternalService;
import com.fafeng.clinic.ai.mapper.AiChatChannelMapper;
import com.fafeng.clinic.ai.mapper.AiEmbeddingChannelMapper;
import com.fafeng.clinic.ai.mapper.ExternalServiceMapper;
import com.fafeng.clinic.ai.vo.ChannelTestResultVO;
import com.fafeng.clinic.ai.vo.ChatChannelVO;
import com.fafeng.clinic.ai.vo.EmbeddingChannelVO;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import com.fafeng.clinic.system.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiChannelService {

    private final AiChatChannelMapper chatChannelMapper;
    private final AiEmbeddingChannelMapper embeddingChannelMapper;
    private final ExternalServiceMapper externalServiceMapper;
    private final SecretEncryptor secretEncryptor;
    private final ChannelRegistry channelRegistry;
    private final ExternalServiceConfigService externalServiceConfigService;
    private final ChatChannelFactory chatChannelFactory;
    private final EmbeddingChannelFactory embeddingChannelFactory;
    private final EnvBootstrapChannelSource envBootstrapChannelSource;
    private final ClinicAiProperties aiProperties;
    private final ClinicEmbeddingProperties embeddingProperties;
    private final AuditLogService auditLogService;

    public List<ChatChannelVO> listChatChannels() {
        if (!channelRegistry.isDbChatBacked()) {
            return envBootstrapChannelSource.loadChatChannels().stream()
                    .map(this::toChatVoFromConfig)
                    .toList();
        }
        return chatChannelMapper.selectList(new LambdaQueryWrapper<AiChatChannel>()
                        .orderByAsc(AiChatChannel::getPriority)
                        .orderByAsc(AiChatChannel::getId))
                .stream()
                .map(this::toChatVo)
                .toList();
    }

    public List<EmbeddingChannelVO> listEmbeddingChannels() {
        if (!channelRegistry.isDbEmbeddingBacked()) {
            return envBootstrapChannelSource.loadEmbeddingChannels().stream()
                    .map(this::toEmbeddingVoFromConfig)
                    .toList();
        }
        return embeddingChannelMapper.selectList(new LambdaQueryWrapper<AiEmbeddingChannel>()
                        .orderByAsc(AiEmbeddingChannel::getPriority)
                        .orderByAsc(AiEmbeddingChannel::getId))
                .stream()
                .map(this::toEmbeddingVo)
                .toList();
    }

    @Transactional
    public ChatChannelVO createChatChannel(SaveChatChannelRequest request) {
        secretEncryptor.requireEncryptionForPersist();
        if (request.apiKey() == null || request.apiKey().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写 API 密钥");
        }
        ensureUniqueChatId(request.channelId(), null);
        AiChatChannel row = new AiChatChannel();
        applyChatRequest(row, request, true);
        row.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        row.setUpdatedAt(row.getCreatedAt());
        chatChannelMapper.insert(row);
        refreshAll();
        auditLogService.log("CREATE_AI_CHAT_CHANNEL", "ai_chat_channel", row.getId(),
                "{\"channelId\":\"" + request.channelId() + "\"}");
        return toChatVo(row);
    }

    @Transactional
    public ChatChannelVO updateChatChannel(String channelId, SaveChatChannelRequest request) {
        secretEncryptor.requireEncryptionForPersist();
        AiChatChannel row = requireChatChannel(channelId);
        ensureUniqueChatId(request.channelId(), channelId);
        applyChatRequest(row, request, request.apiKey() != null && !request.apiKey().isBlank());
        row.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        chatChannelMapper.updateById(row);
        refreshAll();
        auditLogService.log("UPDATE_AI_CHAT_CHANNEL", "ai_chat_channel", row.getId(),
                "{\"channelId\":\"" + channelId + "\"}");
        return toChatVo(row);
    }

    @Transactional
    public void deleteChatChannel(String channelId) {
        AiChatChannel row = requireChatChannel(channelId);
        chatChannelMapper.deleteById(row.getId());
        refreshAll();
        auditLogService.log("DELETE_AI_CHAT_CHANNEL", "ai_chat_channel", row.getId(),
                "{\"channelId\":\"" + channelId + "\"}");
    }

    @Transactional
    public void reorderChatChannels(ReorderChannelsRequest request) {
        int priority = 1;
        for (String channelId : request.channelIds()) {
            AiChatChannel row = requireChatChannel(channelId);
            row.setPriority(priority++);
            row.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            chatChannelMapper.updateById(row);
        }
        refreshAll();
        auditLogService.log("REORDER_AI_CHAT_CHANNELS", "ai_chat_channel", null, "{}");
    }

    public ChannelTestResultVO testChatChannel(String channelId) {
        AiChatChannel row = requireChatChannel(channelId);
        ChatChannelConfig config = toChatConfig(row);
        if (!config.isUsable()) {
            return new ChannelTestResultVO(false, "通道配置不完整");
        }
        try {
            var client = chatChannelFactory.buildClient(config);
            String answer = client.prompt()
                    .system("You are a helpful assistant.")
                    .user("ping")
                    .call()
                    .content();
            if (answer == null || answer.isBlank()) {
                return new ChannelTestResultVO(false, "未返回有效内容");
            }
            return new ChannelTestResultVO(true, "连接测试成功");
        } catch (Exception ex) {
            return new ChannelTestResultVO(false, "连接失败：" + ex.getMessage());
        }
    }

    @Transactional
    public EmbeddingChannelVO createEmbeddingChannel(SaveEmbeddingChannelRequest request) {
        secretEncryptor.requireEncryptionForPersist();
        if (request.apiKey() == null || request.apiKey().isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请填写 API 密钥");
        }
        validateEmbeddingDimensions(request.dimensions(), null);
        ensureUniqueEmbeddingId(request.channelId(), null);
        AiEmbeddingChannel row = new AiEmbeddingChannel();
        applyEmbeddingRequest(row, request, true);
        row.setCreatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        row.setUpdatedAt(row.getCreatedAt());
        embeddingChannelMapper.insert(row);
        refreshAll();
        auditLogService.log("CREATE_AI_EMBEDDING_CHANNEL", "ai_embedding_channel", row.getId(),
                "{\"channelId\":\"" + request.channelId() + "\"}");
        return toEmbeddingVo(row);
    }

    @Transactional
    public EmbeddingChannelVO updateEmbeddingChannel(String channelId, SaveEmbeddingChannelRequest request) {
        secretEncryptor.requireEncryptionForPersist();
        AiEmbeddingChannel row = requireEmbeddingChannel(channelId);
        validateEmbeddingDimensions(request.dimensions(), channelId);
        ensureUniqueEmbeddingId(request.channelId(), channelId);
        applyEmbeddingRequest(row, request, request.apiKey() != null && !request.apiKey().isBlank());
        row.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
        embeddingChannelMapper.updateById(row);
        refreshAll();
        auditLogService.log("UPDATE_AI_EMBEDDING_CHANNEL", "ai_embedding_channel", row.getId(),
                "{\"channelId\":\"" + channelId + "\"}");
        return toEmbeddingVo(row);
    }

    @Transactional
    public void deleteEmbeddingChannel(String channelId) {
        AiEmbeddingChannel row = requireEmbeddingChannel(channelId);
        embeddingChannelMapper.deleteById(row.getId());
        refreshAll();
        auditLogService.log("DELETE_AI_EMBEDDING_CHANNEL", "ai_embedding_channel", row.getId(),
                "{\"channelId\":\"" + channelId + "\"}");
    }

    @Transactional
    public void reorderEmbeddingChannels(ReorderChannelsRequest request) {
        int priority = 1;
        for (String channelId : request.channelIds()) {
            AiEmbeddingChannel row = requireEmbeddingChannel(channelId);
            row.setPriority(priority++);
            row.setUpdatedAt(OffsetDateTime.now(ZoneOffset.UTC));
            embeddingChannelMapper.updateById(row);
        }
        refreshAll();
        auditLogService.log("REORDER_AI_EMBEDDING_CHANNELS", "ai_embedding_channel", null, "{}");
    }

    public ChannelTestResultVO testEmbeddingChannel(String channelId) {
        AiEmbeddingChannel row = requireEmbeddingChannel(channelId);
        EmbeddingChannelConfig config = toEmbeddingConfig(row);
        if (!config.isUsable()) {
            return new ChannelTestResultVO(false, "通道配置不完整");
        }
        try {
            EmbeddingModel model = embeddingChannelFactory.buildModel(config);
            float[] vector = model.embed("ping");
            if (vector == null || vector.length != config.dimensions()) {
                return new ChannelTestResultVO(false, "向量维度与配置不一致");
            }
            return new ChannelTestResultVO(true, "连接测试成功");
        } catch (Exception ex) {
            return new ChannelTestResultVO(false, "连接失败：" + ex.getMessage());
        }
    }

    @Transactional
    public void importFromEnv() {
        if (channelRegistry.isDbChatBacked() || channelRegistry.isDbEmbeddingBacked()
                || externalServiceConfigService.isDbBacked()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "数据库已有配置，无法从环境导入");
        }
        secretEncryptor.requireEncryptionForPersist();
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        insertExternalService(ExternalService.CODE_CHAT, aiProperties.isEnabled(), "", now);
        insertExternalService(ExternalService.CODE_EMBEDDING, embeddingProperties.isEnabled(), "", now);
        String whisperUrl = externalServiceConfigService.getWhisperUrl();
        insertExternalService(ExternalService.CODE_WHISPER, externalServiceConfigService.isWhisperEnabled(), whisperUrl, now);
        String ocrUrl = externalServiceConfigService.getOcrUrl();
        insertExternalService(ExternalService.CODE_OCR, externalServiceConfigService.isOcrEnabled(), ocrUrl, now);

        for (ChatChannelConfig config : envBootstrapChannelSource.loadChatChannels()) {
            AiChatChannel row = new AiChatChannel();
            row.setChannelId(config.channelId());
            row.setDisplayName(config.displayName());
            row.setPriority(config.priority());
            row.setEnabled(config.enabled());
            row.setBaseUrl(config.baseUrl());
            row.setApiKeyEnc(secretEncryptor.encrypt(config.apiKey()));
            row.setModel(config.model());
            row.setTemperature(config.temperature() == null ? new BigDecimal("0.2") : config.temperature());
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            chatChannelMapper.insert(row);
        }
        for (EmbeddingChannelConfig config : envBootstrapChannelSource.loadEmbeddingChannels()) {
            AiEmbeddingChannel row = new AiEmbeddingChannel();
            row.setChannelId(config.channelId());
            row.setDisplayName(config.displayName());
            row.setPriority(config.priority());
            row.setEnabled(config.enabled());
            row.setBaseUrl(config.baseUrl());
            row.setApiKeyEnc(secretEncryptor.encrypt(config.apiKey()));
            row.setModel(config.model());
            row.setDimensions(config.dimensions());
            row.setCreatedAt(now);
            row.setUpdatedAt(now);
            embeddingChannelMapper.insert(row);
        }
        refreshAll();
        auditLogService.log("IMPORT_AI_CHANNELS_FROM_ENV", "ai_chat_channel", null, "{}");
    }

    private void insertExternalService(String code, boolean enabled, String endpointUrl, OffsetDateTime now) {
        ExternalService row = new ExternalService();
        row.setServiceCode(code);
        row.setEnabled(enabled);
        row.setEndpointUrl(endpointUrl == null ? "" : endpointUrl);
        row.setUpdatedAt(now);
        externalServiceMapper.insert(row);
    }

    private void refreshAll() {
        externalServiceConfigService.refresh();
        channelRegistry.refresh();
    }

    private AiChatChannel requireChatChannel(String channelId) {
        AiChatChannel row = chatChannelMapper.selectOne(new LambdaQueryWrapper<AiChatChannel>()
                .eq(AiChatChannel::getChannelId, channelId));
        if (row == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "对话接口不存在");
        }
        return row;
    }

    private AiEmbeddingChannel requireEmbeddingChannel(String channelId) {
        AiEmbeddingChannel row = embeddingChannelMapper.selectOne(new LambdaQueryWrapper<AiEmbeddingChannel>()
                .eq(AiEmbeddingChannel::getChannelId, channelId));
        if (row == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "向量接口不存在");
        }
        return row;
    }

    private void ensureUniqueChatId(String channelId, String excludeChannelId) {
        AiChatChannel existing = chatChannelMapper.selectOne(new LambdaQueryWrapper<AiChatChannel>()
                .eq(AiChatChannel::getChannelId, channelId));
        if (existing != null && (excludeChannelId == null || !existing.getChannelId().equals(excludeChannelId))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "接口标识已存在");
        }
    }

    private void ensureUniqueEmbeddingId(String channelId, String excludeChannelId) {
        AiEmbeddingChannel existing = embeddingChannelMapper.selectOne(new LambdaQueryWrapper<AiEmbeddingChannel>()
                .eq(AiEmbeddingChannel::getChannelId, channelId));
        if (existing != null && (excludeChannelId == null || !existing.getChannelId().equals(excludeChannelId))) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "接口标识已存在");
        }
    }

    private void validateEmbeddingDimensions(int dimensions, String excludeChannelId) {
        List<AiEmbeddingChannel> existing = embeddingChannelMapper.selectList(null);
        for (AiEmbeddingChannel row : existing) {
            if (excludeChannelId != null && excludeChannelId.equals(row.getChannelId())) {
                continue;
            }
            if (row.getDimensions() != null && row.getDimensions() != dimensions) {
                throw new BusinessException(ErrorCode.BAD_REQUEST,
                        "向量维度须与已有通道一致（当前 " + row.getDimensions() + "）");
            }
        }
    }

    private void applyChatRequest(AiChatChannel row, SaveChatChannelRequest request, boolean updateKey) {
        row.setChannelId(request.channelId().trim());
        row.setDisplayName(request.displayName().trim());
        row.setPriority(request.priority());
        row.setEnabled(Boolean.TRUE.equals(request.enabled()));
        row.setBaseUrl(request.baseUrl().trim());
        if (updateKey) {
            row.setApiKeyEnc(secretEncryptor.encrypt(request.apiKey().trim()));
        }
        row.setModel(request.model().trim());
        row.setTemperature(request.temperature() == null ? new BigDecimal("0.2") : request.temperature());
    }

    private void applyEmbeddingRequest(AiEmbeddingChannel row, SaveEmbeddingChannelRequest request, boolean updateKey) {
        row.setChannelId(request.channelId().trim());
        row.setDisplayName(request.displayName().trim());
        row.setPriority(request.priority());
        row.setEnabled(Boolean.TRUE.equals(request.enabled()));
        row.setBaseUrl(request.baseUrl().trim());
        if (updateKey) {
            row.setApiKeyEnc(secretEncryptor.encrypt(request.apiKey().trim()));
        }
        row.setModel(request.model().trim());
        row.setDimensions(request.dimensions());
    }

    private ChatChannelVO toChatVo(AiChatChannel row) {
        String masked = SecretEncryptor.maskApiKey(decryptKey(row.getApiKeyEnc()));
        return new ChatChannelVO(
                row.getChannelId(),
                row.getDisplayName(),
                row.getPriority() == null ? 1 : row.getPriority(),
                Boolean.TRUE.equals(row.getEnabled()),
                row.getBaseUrl(),
                masked,
                row.getModel(),
                row.getTemperature() == null ? new BigDecimal("0.2") : row.getTemperature());
    }

    private ChatChannelVO toChatVoFromConfig(ChatChannelConfig config) {
        return new ChatChannelVO(
                config.channelId(),
                config.displayName(),
                config.priority(),
                config.enabled(),
                config.baseUrl(),
                SecretEncryptor.maskApiKey(config.apiKey()),
                config.model(),
                config.temperature());
    }

    private EmbeddingChannelVO toEmbeddingVo(AiEmbeddingChannel row) {
        return new EmbeddingChannelVO(
                row.getChannelId(),
                row.getDisplayName(),
                row.getPriority() == null ? 1 : row.getPriority(),
                Boolean.TRUE.equals(row.getEnabled()),
                row.getBaseUrl(),
                SecretEncryptor.maskApiKey(decryptKey(row.getApiKeyEnc())),
                row.getModel(),
                row.getDimensions() == null ? 1024 : row.getDimensions());
    }

    private EmbeddingChannelVO toEmbeddingVoFromConfig(EmbeddingChannelConfig config) {
        return new EmbeddingChannelVO(
                config.channelId(),
                config.displayName(),
                config.priority(),
                config.enabled(),
                config.baseUrl(),
                SecretEncryptor.maskApiKey(config.apiKey()),
                config.model(),
                config.dimensions());
    }

    private ChatChannelConfig toChatConfig(AiChatChannel row) {
        return new ChatChannelConfig(
                row.getChannelId(),
                row.getDisplayName(),
                row.getPriority() == null ? 1 : row.getPriority(),
                Boolean.TRUE.equals(row.getEnabled()),
                row.getBaseUrl(),
                decryptKey(row.getApiKeyEnc()),
                row.getModel(),
                row.getTemperature() == null ? new BigDecimal("0.2") : row.getTemperature());
    }

    private EmbeddingChannelConfig toEmbeddingConfig(AiEmbeddingChannel row) {
        return new EmbeddingChannelConfig(
                row.getChannelId(),
                row.getDisplayName(),
                row.getPriority() == null ? 1 : row.getPriority(),
                Boolean.TRUE.equals(row.getEnabled()),
                row.getBaseUrl(),
                decryptKey(row.getApiKeyEnc()),
                row.getModel(),
                row.getDimensions() == null ? 1024 : row.getDimensions());
    }

    private String decryptKey(String apiKeyEnc) {
        if (apiKeyEnc == null || apiKeyEnc.isBlank()) {
            return "";
        }
        if (secretEncryptor.isEncryptionAvailable()) {
            return secretEncryptor.decrypt(apiKeyEnc);
        }
        return apiKeyEnc;
    }
}
