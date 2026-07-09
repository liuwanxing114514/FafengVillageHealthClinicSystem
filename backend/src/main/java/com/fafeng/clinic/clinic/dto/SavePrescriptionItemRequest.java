package com.fafeng.clinic.clinic.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record SavePrescriptionItemRequest(
        @NotNull Long medicineId,
        @NotNull @DecimalMin("0.001") BigDecimal quantity,
        @NotBlank String unit,
        String usage
) {
}
