package com.fafeng.clinic.clinic.vo;

import java.math.BigDecimal;

public record PrescriptionItemVO(
        Long id,
        Long medicineId,
        String dosageForm,
        String medicineName,
        String specification,
        BigDecimal quantity,
        String unit,
        String usage,
        Integer sortOrder
) {
}
