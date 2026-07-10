package com.fafeng.clinic.ai.vo;

public record EmbeddingChannelVO(
        String channelId,
        String displayName,
        int priority,
        boolean enabled,
        String baseUrl,
        String apiKeyMasked,
        String model,
        int dimensions) {
}
