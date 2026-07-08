package com.fafeng.clinic.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AdjustRequest(
        @NotNull Long medicineId,
        @NotNull Long batchId,
        @NotNull BigDecimal quantityChange,
        @NotBlank String unit,
        @NotBlank String reason,
        String remark
) {
}
