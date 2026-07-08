package com.fafeng.clinic.inventory.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LowStockAlertVO(
        Long medicineId,
        String medicineName,
        String specification,
        BigDecimal currentQuantity,
        BigDecimal threshold,
        String baseUnit
) {
}
