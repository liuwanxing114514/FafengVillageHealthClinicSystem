package com.fafeng.clinic.ai.vo;

import java.math.BigDecimal;

public record ChatChannelVO(
        String channelId,
        String displayName,
        int priority,
        boolean enabled,
        String baseUrl,
        String apiKeyMasked,
        String model,
        BigDecimal temperature) {
}
