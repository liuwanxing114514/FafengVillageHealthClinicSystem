package com.fafeng.clinic.inventory.vo;

import java.math.BigDecimal;
import java.util.List;

public record OutboundPreviewLineVO(
        Long medicineId,
        String medicineName,
        BigDecimal requestedQuantity,
        String unit,
        BigDecimal requestedBaseQuantity,
        String baseUnit,
        boolean sufficient,
        List<BatchAllocationVO> recommendedAllocations
) {
}
