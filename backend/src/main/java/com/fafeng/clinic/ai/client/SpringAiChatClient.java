package com.fafeng.clinic.ai.client;

import com.fafeng.clinic.ai.config.ClinicAiProperties;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

import java.util.function.Supplier;

/**
 * 基于 Spring AI {@link ChatClient} 的 DeepSeek（OpenAI 兼容）实现。
 * 主通道（如硅基流动）限流/拥堵时，可自动切换 DeepSeek 官方 API 兜底。
 */
public class SpringAiChatClient implements AiChatClient {

    private static final Logger log = LoggerFactory.getLogger(SpringAiChatClient.class);

    private final ClinicAiProperties properties;
    private final ChatClient primaryChatClient;
    private final ChatClient fallbackChatClient;

    public SpringAiChatClient(
            ClinicAiProperties properties,
            ChatClient primaryChatClient,
            ChatClient fallbackChatClient) {
        this.properties = properties;
        this.primaryChatClient = primaryChatClient;
        this.fallbackChatClient = fallbackChatClient;
    }

    @Override
    public boolean isConfigured() {
        String apiKey = properties.getDeepseekApiKey();
        return properties.isEnabled()
                && apiKey != null
                && !apiKey.isBlank();
    }

    @Override
    public String chatCompletion(String systemPrompt, String userMessage) {
        requireConfigured();
        return invokeWithFallback(
                () -> primaryChatClient.prompt()
                        .system(systemPrompt)
                        .user(userMessage)
                        .call()
                        .content(),
                () -> fallbackChatClient.prompt()
                        .system(systemPrompt)
                        .user(userMessage)
                        .call()
                        .content(),
                "chat");
    }

    @Override
    public String chatWithTools(String systemPrompt, String userMessage, Object toolSource) {
        requireConfigured();
        return invokeWithFallback(
                () -> primaryChatClient.prompt()
                        .system(systemPrompt)
                        .user(userMessage)
                        .tools(toolSource)
                        .call()
                        .content(),
                () -> fallbackChatClient.prompt()
                        .system(systemPrompt)
                        .user(userMessage)
                        .tools(toolSource)
                        .call()
                        .content(),
                "agent");
    }

    private String invokeWithFallback(
            Supplier<String> primaryCall,
            Supplier<String> fallbackCall,
            String scene) {
        try {
            return requireContent(primaryCall.get());
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception primaryEx) {
            if (fallbackChatClient != null && AiClientExceptionMapper.isFallbackEligible(primaryEx)) {
                log.warn("Primary AI {} failed ({}), switching to DeepSeek fallback",
                        scene, primaryEx.getMessage());
                try {
                    return requireContent(fallbackCall.get());
                } catch (BusinessException ex) {
                    throw ex;
                } catch (Exception fallbackEx) {
                    log.warn("DeepSeek fallback {} also failed: {}", scene, fallbackEx.getMessage());
                    throw AiClientExceptionMapper.toBusinessException(fallbackEx);
                }
            }
            log.warn("Spring AI {} failed: {}", scene, primaryEx.getMessage());
            throw AiClientExceptionMapper.toBusinessException(primaryEx);
        }
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "DeepSeek 未配置");
        }
    }

    private String requireContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 未返回有效内容");
        }
        return content.trim();
    }
}
