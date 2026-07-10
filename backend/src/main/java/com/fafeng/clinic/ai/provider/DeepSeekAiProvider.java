package com.fafeng.clinic.ai.provider;

import com.fafeng.clinic.ai.client.AiChatClient;
import com.fafeng.clinic.ai.config.ExternalServiceConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeepSeekAiProvider implements AiProvider {

    private final ExternalServiceConfigService externalServiceConfigService;
    private final AiChatClient aiChatClient;

    @Override
    public String name() {
        return "deepseek";
    }

    @Override
    public boolean isAvailable() {
        return externalServiceConfigService.isChatEnabled() && aiChatClient.isConfigured();
    }

    @Override
    public String chatCompletion(String systemPrompt, String userMessage) {
        return aiChatClient.chatCompletion(systemPrompt, userMessage);
    }

    @Override
    public String chatCompletion(String systemPrompt, String userMessage, boolean skipDesensitization) {
        return aiChatClient.chatCompletion(systemPrompt, userMessage, skipDesensitization);
    }
}
