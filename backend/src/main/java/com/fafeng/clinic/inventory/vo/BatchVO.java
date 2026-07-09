package com.fafeng.clinic.inventory.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record BatchVO(
        Long id,
        Long medicineId,
        String medicineName,
        String batchNo,
        LocalDate expiryDate,
        BigDecimal quantity,
        String baseUnit,
        BigDecimal purchasePrice,
        String supplier,
        String status,
        OffsetDateTime createdAt
) {
}
