package com.fafeng.clinic.ai.provider;

import com.fafeng.clinic.ai.config.ClinicAiProperties;
import org.springframework.stereotype.Component;

@Component
public class DeepSeekAiProvider implements AiProvider {

    private final ClinicAiProperties properties;

    public DeepSeekAiProvider(ClinicAiProperties properties) {
        this.properties = properties;
    }

    @Override
    public String name() {
        return "deepseek";
    }

    @Override
    public boolean isAvailable() {
        return properties.isEnabled()
                && properties.getDeepseekApiKey() != null
                && !properties.getDeepseekApiKey().isBlank();
    }
}
