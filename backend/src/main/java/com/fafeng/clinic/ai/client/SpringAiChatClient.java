package com.fafeng.clinic.ai.client;

import com.fafeng.clinic.ai.config.ClinicAiProperties;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;

/**
 * 基于 Spring AI {@link ChatClient} 的 DeepSeek（OpenAI 兼容）实现。
 */
@RequiredArgsConstructor
public class SpringAiChatClient implements AiChatClient {

    private static final Logger log = LoggerFactory.getLogger(SpringAiChatClient.class);

    private final ClinicAiProperties properties;
    private final ChatClient chatClient;

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
        try {
            String content = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .call()
                    .content();
            return requireContent(content);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Spring AI chat failed: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 服务暂不可用，请稍后重试或手动录入");
        }
    }

    @Override
    public String chatWithTools(String systemPrompt, String userMessage, Object toolSource) {
        requireConfigured();
        try {
            String content = chatClient.prompt()
                    .system(systemPrompt)
                    .user(userMessage)
                    .tools(toolSource)
                    .call()
                    .content();
            return requireContent(content);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Spring AI agent chat failed: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 服务暂不可用，请稍后重试");
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
