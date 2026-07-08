package com.fafeng.clinic.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OutboundConfirmRequest(
        @NotNull Long patientId,
        @NotNull Long prescriptionId,
        @NotNull Long medicineId,
        @NotEmpty @Valid List<OutboundAllocationRequest> allocations
) {
}
