package com.fafeng.clinic.inventory.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ExpiringAlertVO(
        Long batchId,
        Long medicineId,
        String medicineName,
        String batchNo,
        LocalDate expiryDate,
        BigDecimal quantity,
        String baseUnit
) {
}
