package com.fafeng.clinic.inventory.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record FlowVO(
        Long id,
        Long medicineId,
        String medicineName,
        Long batchId,
        String batchNo,
        String flowType,
        BigDecimal quantityChange,
        BigDecimal quantityBefore,
        BigDecimal quantityAfter,
        String unit,
        Long patientId,
        Long prescriptionId,
        String reason,
        String remark,
        String operator,
        OffsetDateTime createdAt
) {
}
