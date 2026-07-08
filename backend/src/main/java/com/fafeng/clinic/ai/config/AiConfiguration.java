package com.fafeng.clinic.ai.config;

import com.fafeng.clinic.ai.provider.AiProvider;
import com.fafeng.clinic.ai.provider.DeepSeekAiProvider;
import com.fafeng.clinic.ai.provider.LocalAiProvider;
import com.fafeng.clinic.ai.provider.NoopAiProvider;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;
import java.util.Map;

@Configuration
@EnableConfigurationProperties({ClinicAiProperties.class, ClinicVoiceProperties.class})
public class AiConfiguration {

    @Bean
    public Map<String, AiProvider> aiProviders(NoopAiProvider noop,
                                               DeepSeekAiProvider deepseek,
                                               LocalAiProvider local) {
        return Map.of(
                noop.name(), noop,
                deepseek.name(), deepseek,
                local.name(), local
        );
    }

    @Bean
    public AiProvider activeAiProvider(ClinicAiProperties properties, Map<String, AiProvider> aiProviders) {
        if (!properties.isEnabled()) {
            return aiProviders.get("noop");
        }
        String providerName = properties.getProvider() == null
                ? "noop"
                : properties.getProvider().trim().toLowerCase(Locale.ROOT);
        return aiProviders.getOrDefault(providerName, aiProviders.get("noop"));
    }
}
