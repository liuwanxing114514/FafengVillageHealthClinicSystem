package com.fafeng.clinic.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OutboundPreviewRequest(
        @NotNull Long patientId,
        @NotNull Long prescriptionId,
        @NotEmpty @Valid List<OutboundLineRequest> items
) {
}
