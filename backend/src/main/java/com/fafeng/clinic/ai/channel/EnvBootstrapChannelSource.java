package com.fafeng.clinic.ai.channel;

import com.fafeng.clinic.ai.config.ClinicAiProperties;
import com.fafeng.clinic.ai.config.ClinicEmbeddingProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 通道配置来源：数据库无通道行时，从 {@code DEEPSEEK_*}/{@code CLINIC_EMBEDDING_*} 构造虚拟通道。
 * Chat 主通道 priority=1，{@code DEEPSEEK_FALLBACK_*} 为 priority=2（可选）。
 */
@Component
public class EnvBootstrapChannelSource implements ChannelSource {

    private final ClinicAiProperties aiProperties;
    private final ClinicEmbeddingProperties embeddingProperties;
    private final DbChannelSource dbChannelSource;

    public EnvBootstrapChannelSource(ClinicAiProperties aiProperties,
                                     ClinicEmbeddingProperties embeddingProperties,
                                     DbChannelSource dbChannelSource) {
        this.aiProperties = aiProperties;
        this.embeddingProperties = embeddingProperties;
        this.dbChannelSource = dbChannelSource;
    }

    @Override
    public boolean hasDbChatChannels() {
        return dbChannelSource.hasDbChatChannels();
    }

    @Override
    public boolean hasDbEmbeddingChannels() {
        return dbChannelSource.hasDbEmbeddingChannels();
    }

    @Override
    public List<ChatChannelConfig> loadChatChannels() {
        if (dbChannelSource.hasDbChatChannels()) {
            return dbChannelSource.loadChatChannels();
        }
        List<ChatChannelConfig> channels = new ArrayList<>();
        String primaryKey = aiProperties.getDeepseekApiKey();
        if (primaryKey != null && !primaryKey.isBlank()) {
            channels.add(new ChatChannelConfig(
                    "env-primary",
                    "环境变量主通道",
                    1,
                    true,
                    aiProperties.getDeepseekBaseUrl(),
                    primaryKey.trim(),
                    aiProperties.getDeepseekModel(),
                    new BigDecimal("0.2")));
        }
        if (aiProperties.hasDeepseekFallback()) {
            channels.add(new ChatChannelConfig(
                    "env-fallback",
                    "环境变量备用通道",
                    2,
                    true,
                    aiProperties.getDeepseekFallbackBaseUrl(),
                    aiProperties.getDeepseekFallbackApiKey().trim(),
                    aiProperties.getDeepseekFallbackModel(),
                    new BigDecimal("0.2")));
        }
        return List.copyOf(channels);
    }

    @Override
    public List<EmbeddingChannelConfig> loadEmbeddingChannels() {
        if (dbChannelSource.hasDbEmbeddingChannels()) {
            return dbChannelSource.loadEmbeddingChannels();
        }
        if (!embeddingProperties.isConfigured()) {
            return List.of();
        }
        return List.of(new EmbeddingChannelConfig(
                "env-primary",
                "环境变量向量通道",
                1,
                embeddingProperties.isEnabled(),
                embeddingProperties.resolveBaseUrl(),
                embeddingProperties.resolveApiKey(),
                embeddingProperties.getModel(),
                embeddingProperties.getDimensions()));
    }
}
