package com.fafeng.clinic.clinic.vo;

import java.math.BigDecimal;

public record OutboundDraftItemVO(
        Long medicineId,
        String medicineName,
        String specification,
        BigDecimal quantity,
        String unit,
        String usage
) {
}
