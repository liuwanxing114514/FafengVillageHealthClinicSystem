package com.fafeng.clinic.ai.channel;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class EmbeddingChannelFactory {

    public List<EmbeddingChannelRuntime> buildRuntimes(List<EmbeddingChannelConfig> configs) {
        List<EmbeddingChannelRuntime> runtimes = new ArrayList<>();
        for (EmbeddingChannelConfig config : configs) {
            if (!config.isUsable()) {
                continue;
            }
            runtimes.add(new EmbeddingChannelRuntime(config, buildModel(config)));
        }
        return List.copyOf(runtimes);
    }

    public EmbeddingModel buildModel(EmbeddingChannelConfig config) {
        String baseUrl = ChatChannelFactory.normalizeBaseUrl(config.baseUrl());
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(config.apiKey().trim())
                .build();
        return new OpenAiEmbeddingModel(
                openAiApi,
                MetadataMode.EMBED,
                OpenAiEmbeddingOptions.builder()
                        .model(config.model())
                        .build());
    }
}
