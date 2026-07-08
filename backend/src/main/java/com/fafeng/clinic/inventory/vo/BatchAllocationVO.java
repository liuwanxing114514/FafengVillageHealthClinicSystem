package com.fafeng.clinic.inventory.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BatchAllocationVO(
        Long batchId,
        String batchNo,
        LocalDate expiryDate,
        BigDecimal availableQuantity,
        BigDecimal recommendedQuantity
) {
}
