package com.fafeng.clinic.ai.channel;

import java.math.BigDecimal;

public record ChatChannelConfig(
        String channelId,
        String displayName,
        int priority,
        boolean enabled,
        String baseUrl,
        String apiKey,
        String model,
        BigDecimal temperature) {

    public String label() {
        return displayName != null && !displayName.isBlank() ? displayName : channelId;
    }

    public boolean isUsable() {
        return enabled
                && baseUrl != null && !baseUrl.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && model != null && !model.isBlank();
    }
}
