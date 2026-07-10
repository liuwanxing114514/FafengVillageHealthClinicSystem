package com.fafeng.clinic.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SaveChatChannelRequest(
        @NotBlank String channelId,
        @NotBlank String displayName,
        @NotNull Integer priority,
        @NotNull Boolean enabled,
        @NotBlank String baseUrl,
        String apiKey,
        @NotBlank String model,
        BigDecimal temperature) {
}
