package com.fafeng.clinic.ai.config;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 病历向量化 Embedding 条件装配（v2.2）。openai / local 均走 OpenAI 兼容 API。
 */
@Configuration
@ConditionalOnProperty(name = "clinic.ai.embedding.enabled", havingValue = "true")
public class EmbeddingConfiguration {

    @Bean
    public EmbeddingModel clinicEmbeddingModel(ClinicEmbeddingProperties properties) {
        String baseUrl = properties.resolveBaseUrl().replaceAll("/+$", "");
        String apiKey = properties.resolveApiKey();
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
        return new OpenAiEmbeddingModel(
                openAiApi,
                MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder()
                        .model(properties.getModel())
                        .build());
    }
}
