package com.fafeng.clinic.ai.channel;

import com.fafeng.clinic.ai.config.ClinicAiProperties;
import com.fafeng.clinic.ai.config.ClinicEmbeddingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EnvBootstrapChannelSourceTest {

    @Mock
    private DbChannelSource dbChannelSource;

    private ClinicAiProperties aiProperties;
    private ClinicEmbeddingProperties embeddingProperties;
    private EnvBootstrapChannelSource source;

    @BeforeEach
    void setUp() {
        aiProperties = new ClinicAiProperties();
        embeddingProperties = new ClinicEmbeddingProperties();
        source = new EnvBootstrapChannelSource(aiProperties, embeddingProperties, dbChannelSource);
    }

    @Test
    void loadsPrimaryAndFallbackChatChannelsFromEnv() {
        when(dbChannelSource.hasDbChatChannels()).thenReturn(false);
        aiProperties.setDeepseekApiKey("primary-key");
        aiProperties.setDeepseekBaseUrl("https://api.siliconflow.cn");
        aiProperties.setDeepseekModel("deepseek-ai/DeepSeek-V4-Pro");
        aiProperties.setDeepseekFallbackApiKey("fallback-key");

        List<ChatChannelConfig> channels = source.loadChatChannels();

        assertEquals(2, channels.size());
        assertEquals(1, channels.get(0).priority());
        assertEquals("env-primary", channels.get(0).channelId());
        assertEquals(2, channels.get(1).priority());
        assertEquals("env-fallback", channels.get(1).channelId());
    }

    @Test
    void loadsEmbeddingChannelFromEnvWhenConfigured() {
        when(dbChannelSource.hasDbEmbeddingChannels()).thenReturn(false);
        embeddingProperties.setEnabled(true);
        embeddingProperties.setApiKey("embed-key");
        embeddingProperties.setBaseUrl("https://api.siliconflow.cn/v1");
        embeddingProperties.setModel("BAAI/bge-m3");
        embeddingProperties.setDimensions(1024);

        List<EmbeddingChannelConfig> channels = source.loadEmbeddingChannels();

        assertEquals(1, channels.size());
        assertTrue(channels.get(0).isUsable());
        assertEquals(1024, channels.get(0).dimensions());
    }

    @Test
    void delegatesToDbWhenChatChannelsExist() {
        when(dbChannelSource.hasDbChatChannels()).thenReturn(true);
        when(dbChannelSource.loadChatChannels()).thenReturn(List.of(
                new ChatChannelConfig("db-1", "DB", 1, true, "url", "key", "model", new java.math.BigDecimal("0.2"))));

        assertEquals(1, source.loadChatChannels().size());
        assertEquals("db-1", source.loadChatChannels().getFirst().channelId());
    }
}
