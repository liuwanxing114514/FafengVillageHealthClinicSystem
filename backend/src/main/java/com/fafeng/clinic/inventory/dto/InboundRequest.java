package com.fafeng.clinic.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record InboundRequest(
        @NotNull Long medicineId,
        @NotNull @DecimalMin("0.001") BigDecimal quantity,
        @NotBlank String unit,
        @NotBlank String batchNo,
        LocalDate expiryDate,
        BigDecimal purchasePrice,
        String supplier,
        String remark
) {
}
