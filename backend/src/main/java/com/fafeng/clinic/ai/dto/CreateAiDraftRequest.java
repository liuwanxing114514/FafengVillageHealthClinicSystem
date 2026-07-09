package com.fafeng.clinic.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateAiDraftRequest(
        @NotBlank String draftType,
        String payload,
        String source
) {
}
