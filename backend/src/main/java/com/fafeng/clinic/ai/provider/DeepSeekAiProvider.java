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
        return properties.isEnabled()
                && deepSeekClient.isConfigured();
    }

    public String chatCompletion(String systemPrompt, String userMessage) {
        if (!isAvailable()) {
            throw new com.fafeng.clinic.common.BusinessException(
                    com.fafeng.clinic.common.ErrorCode.SERVICE_UNAVAILABLE,
                    "DeepSeek AI 未启用或未配置 API Key");
        }
        return deepSeekClient.chatCompletion(systemPrompt, userMessage);
    }
}
