package com.fafeng.clinic.ai.channel;

public record EmbeddingChannelConfig(
        String channelId,
        String displayName,
        int priority,
        boolean enabled,
        String baseUrl,
        String apiKey,
        String model,
        int dimensions) {

    public String label() {
        return displayName != null && !displayName.isBlank() ? displayName : channelId;
    }

    public boolean isUsable() {
        return enabled
                && baseUrl != null && !baseUrl.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && model != null && !model.isBlank()
                && dimensions > 0;
    }
}
