package com.fafeng.clinic.ai.channel;

import org.springframework.ai.embedding.EmbeddingModel;

public record EmbeddingChannelRuntime(EmbeddingChannelConfig config, EmbeddingModel embeddingModel) {
}
