package com.fafeng.clinic.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record OutboundLineRequest(
        @NotNull Long medicineId,
        @NotNull @DecimalMin("0.001") BigDecimal quantity,
        @NotBlank String unit
) {
}
