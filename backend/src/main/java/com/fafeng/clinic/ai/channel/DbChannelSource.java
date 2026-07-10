package com.fafeng.clinic.ai.channel;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fafeng.clinic.ai.config.SecretEncryptor;
import com.fafeng.clinic.ai.entity.AiChatChannel;
import com.fafeng.clinic.ai.entity.AiEmbeddingChannel;
import com.fafeng.clinic.ai.mapper.AiChatChannelMapper;
import com.fafeng.clinic.ai.mapper.AiEmbeddingChannelMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

@Component
public class DbChannelSource implements ChannelSource {

    private final AiChatChannelMapper chatChannelMapper;
    private final AiEmbeddingChannelMapper embeddingChannelMapper;
    private final SecretEncryptor secretEncryptor;

    public DbChannelSource(AiChatChannelMapper chatChannelMapper,
                           AiEmbeddingChannelMapper embeddingChannelMapper,
                           SecretEncryptor secretEncryptor) {
        this.chatChannelMapper = chatChannelMapper;
        this.embeddingChannelMapper = embeddingChannelMapper;
        this.secretEncryptor = secretEncryptor;
    }

    @Override
    public boolean hasDbChatChannels() {
        return chatChannelMapper.selectCount(null) > 0;
    }

    @Override
    public boolean hasDbEmbeddingChannels() {
        return embeddingChannelMapper.selectCount(null) > 0;
    }

    @Override
    public List<ChatChannelConfig> loadChatChannels() {
        if (!hasDbChatChannels()) {
            return List.of();
        }
        return chatChannelMapper.selectList(new LambdaQueryWrapper<AiChatChannel>()
                        .orderByAsc(AiChatChannel::getPriority)
                        .orderByAsc(AiChatChannel::getId))
                .stream()
                .map(this::toChatConfig)
                .sorted(Comparator.comparingInt(ChatChannelConfig::priority))
                .toList();
    }

    @Override
    public List<EmbeddingChannelConfig> loadEmbeddingChannels() {
        if (!hasDbEmbeddingChannels()) {
            return List.of();
        }
        return embeddingChannelMapper.selectList(new LambdaQueryWrapper<AiEmbeddingChannel>()
                        .orderByAsc(AiEmbeddingChannel::getPriority)
                        .orderByAsc(AiEmbeddingChannel::getId))
                .stream()
                .map(this::toEmbeddingConfig)
                .sorted(Comparator.comparingInt(EmbeddingChannelConfig::priority))
                .toList();
    }

    private ChatChannelConfig toChatConfig(AiChatChannel row) {
        String apiKey = decryptKey(row.getApiKeyEnc());
        BigDecimal temperature = row.getTemperature() == null ? new BigDecimal("0.2") : row.getTemperature();
        return new ChatChannelConfig(
                row.getChannelId(),
                row.getDisplayName(),
                row.getPriority() == null ? 1 : row.getPriority(),
                Boolean.TRUE.equals(row.getEnabled()),
                row.getBaseUrl(),
                apiKey,
                row.getModel(),
                temperature);
    }

    private EmbeddingChannelConfig toEmbeddingConfig(AiEmbeddingChannel row) {
        String apiKey = decryptKey(row.getApiKeyEnc());
        return new EmbeddingChannelConfig(
                row.getChannelId(),
                row.getDisplayName(),
                row.getPriority() == null ? 1 : row.getPriority(),
                Boolean.TRUE.equals(row.getEnabled()),
                row.getBaseUrl(),
                apiKey,
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
