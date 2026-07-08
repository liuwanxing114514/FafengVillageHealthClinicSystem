package com.fafeng.clinic.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OutboundAllocationRequest(
        @NotNull Long batchId,
        @NotNull @DecimalMin("0.001") BigDecimal quantity
) {
}
