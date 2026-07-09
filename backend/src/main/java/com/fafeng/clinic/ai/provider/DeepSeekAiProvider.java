package com.fafeng.clinic.ai.provider;

import com.fafeng.clinic.ai.client.AiChatClient;
import com.fafeng.clinic.ai.config.ClinicAiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DeepSeekAiProvider implements AiProvider {

    private final ClinicAiProperties properties;
    private final AiChatClient aiChatClient;

    @Override
    public String name() {
        return "deepseek";
    }

    @Override
    public boolean isAvailable() {
        return properties.isEnabled() && aiChatClient.isConfigured();
    }

    @Override
    public String chatCompletion(String systemPrompt, String userMessage) {
        return aiChatClient.chatCompletion(systemPrompt, userMessage);
    }
}
