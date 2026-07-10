package com.fafeng.clinic.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SaveEmbeddingChannelRequest(
        @NotBlank String channelId,
        @NotBlank String displayName,
        @NotNull Integer priority,
        @NotNull Boolean enabled,
        @NotBlank String baseUrl,
        String apiKey,
        @NotBlank String model,
        @NotNull Integer dimensions) {
}
