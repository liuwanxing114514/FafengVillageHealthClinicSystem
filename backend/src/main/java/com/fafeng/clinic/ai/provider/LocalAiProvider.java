package com.fafeng.clinic.ai.provider;

import com.fafeng.clinic.ai.config.ClinicAiProperties;
import org.springframework.stereotype.Component;

@Component
public class LocalAiProvider implements AiProvider {

    private final ClinicAiProperties properties;

    public LocalAiProvider(ClinicAiProperties properties) {
        this.properties = properties;
    }

    @Override
    public String name() {
        return "local";
    }

    @Override
    public boolean isAvailable() {
        return properties.isEnabled()
                && properties.getLocalBaseUrl() != null
                && !properties.getLocalBaseUrl().isBlank();
    }
}
