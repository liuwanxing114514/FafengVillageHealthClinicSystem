package com.fafeng.clinic.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveQuickPhraseRequest(
        @NotBlank @Size(max = 32) String fieldKey,
        @NotBlank @Size(max = 2000) String content
) {
}
