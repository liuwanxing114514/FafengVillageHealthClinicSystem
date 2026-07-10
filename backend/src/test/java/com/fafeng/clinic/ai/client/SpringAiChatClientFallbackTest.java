package com.fafeng.clinic.ai.client;

import com.fafeng.clinic.ai.config.ClinicAiProperties;
import com.fafeng.clinic.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.retry.NonTransientAiException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

@ExtendWith(MockitoExtension.class)
class SpringAiChatClientFallbackTest {

    private ClinicAiProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ClinicAiProperties();
        properties.setEnabled(true);
        properties.setDeepseekApiKey("primary-key");
        properties.setDeepseekFallbackApiKey("fallback-key");
    }

    @Test
    void usesFallbackWhenPrimaryRateLimited() {
        ChatClient primary = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        ChatClient fallback = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(primary.prompt().system(anyString()).user(anyString()).call().content())
                .thenThrow(new NonTransientAiException("429 - rate limiting"));
        when(fallback.prompt().system(anyString()).user(anyString()).call().content())
                .thenReturn("fallback answer");

        SpringAiChatClient client = new SpringAiChatClient(properties, primary, fallback);
        assertEquals("fallback answer", client.chatCompletion("sys", "hello"));
    }

    @Test
    void throwsWhenPrimaryAndFallbackFail() {
        ChatClient primary = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        ChatClient fallback = mock(ChatClient.class, RETURNS_DEEP_STUBS);
        when(primary.prompt().system(anyString()).user(anyString()).call().content())
                .thenThrow(new NonTransientAiException("429 - rate limiting"));
        when(fallback.prompt().system(anyString()).user(anyString()).call().content())
                .thenThrow(new NonTransientAiException("503 - unavailable"));

        SpringAiChatClient client = new SpringAiChatClient(properties, primary, fallback);
        BusinessException ex = assertThrows(BusinessException.class, () -> client.chatCompletion("sys", "hello"));
        assertEquals("AI 服务暂不可用，请稍后重试", ex.getMessage());
    }

    @Test
    void fallbackEligibleForSiliconFlowBusyCode() {
        assertTrue(AiClientExceptionMapper.isFallbackEligible(
                new NonTransientAiException("429 - {\"code\":50609,\"message\":\"System is too busy now.\"}")));
    }
}
