package com.fafeng.clinic.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateAiDraftPayloadRequest(
        @NotBlank String payload
) {
}
