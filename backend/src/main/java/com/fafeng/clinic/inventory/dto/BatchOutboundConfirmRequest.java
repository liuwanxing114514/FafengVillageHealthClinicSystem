package com.fafeng.clinic.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BatchOutboundConfirmRequest(
        @NotBlank String reason,
        @NotEmpty @Valid List<BatchOutboundConfirmLineRequest> lines
) {
}
