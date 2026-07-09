package com.fafeng.clinic.ai.provider;

import com.fafeng.clinic.ai.client.DeepSeekClient;
import com.fafeng.clinic.ai.config.ClinicAiProperties;
import org.springframework.stereotype.Component;

@Component
public class DeepSeekAiProvider implements AiProvider {

    private final ClinicAiProperties properties;
    private final DeepSeekClient deepSeekClient;

    public DeepSeekAiProvider(ClinicAiProperties properties, DeepSeekClient deepSeekClient) {
        this.properties = properties;
        this.deepSeekClient = deepSeekClient;
    }

    @Override
    public String name() {
        return "deepseek";
    }

    @Override
    public boolean isAvailable() {
        return properties.isEnabled() && deepSeekClient.isConfigured();
    }

    @Override
    public String chatCompletion(String systemPrompt, String userMessage) {
        return deepSeekClient.chatCompletion(systemPrompt, userMessage);
    }
}
