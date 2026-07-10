package com.fafeng.clinic.ai.client;

import com.fafeng.clinic.ai.channel.ChannelRegistry;
import com.fafeng.clinic.ai.channel.ChatChannelConfig;
import com.fafeng.clinic.ai.channel.ChatChannelRuntime;
import com.fafeng.clinic.ai.config.ExternalServiceConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.NonTransientAiException;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

@ExtendWith(MockitoExtension.class)
class ResilientAiChatClientFallbackTest {

    @Mock
    private ExternalServiceConfigService externalServiceConfigService;
    @Mock
    private ChannelRegistry channelRegistry;

    private ResilientAiChatClient client;

    @BeforeEach
    void setUp() {
        client = new ResilientAiChatClient(externalServiceConfigService, channelRegistry);
    }

    @Test
    void usesFallbackWhenPrimaryRateLimited() {
        when(externalServiceConfigService.isChatEnabled()).thenReturn(true);
        ChatClient primary = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        ChatClient fallback = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(primary.prompt().system(anyString()).user(anyString()).call().content())
                .thenThrow(new NonTransientAiException("429 - rate limiting"));
        when(fallback.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("fallback answer");

        stubChannels(primary, fallback);
        assertEquals("fallback answer", client.chatCompletion("sys", "hello"));
    }

    @Test
    void throwsWhenPrimaryAndFallbackFail() {
        when(externalServiceConfigService.isChatEnabled()).thenReturn(true);
        ChatClient primary = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        ChatClient fallback = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(primary.prompt().system(anyString()).user(anyString()).call().content())
                .thenThrow(new NonTransientAiException("429 - rate limiting"));
        when(fallback.prompt().system(anyString()).user(anyString()).call().content())
                .thenThrow(new NonTransientAiException("503 - unavailable"));

        stubChannels(primary, fallback);
        var ex = assertThrows(com.fafeng.clinic.common.BusinessException.class,
                () -> client.chatCompletion("sys", "hello"));
        assertEquals("AI 服务暂不可用，请稍后重试", ex.getMessage());
    }

    @Test
    void fallbackEligibleForSiliconFlowBusyCode() {
        assertTrue(AiClientExceptionMapper.isFallbackEligible(
                new NonTransientAiException("429 - {\"code\":50609,\"message\":\"System is too busy now.\"}")));
    }

    private void stubChannels(ChatClient primary, ChatClient fallback) {
        ChatChannelConfig primaryConfig = new ChatChannelConfig(
                "primary", "主通道", 1, true,
                "https://api.example.com", "key1", "model", new BigDecimal("0.2"));
        ChatChannelConfig fallbackConfig = new ChatChannelConfig(
                "fallback", "备用通道", 2, true,
                "https://api.deepseek.com", "key2", "model", new BigDecimal("0.2"));
        when(channelRegistry.chatChannels()).thenReturn(List.of(
                new ChatChannelRuntime(primaryConfig, primary),
                new ChatChannelRuntime(fallbackConfig, fallback)));
        when(channelRegistry.hasUsableChatChannels()).thenReturn(true);
    }
}
