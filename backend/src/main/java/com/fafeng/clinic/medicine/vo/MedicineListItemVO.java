package com.fafeng.clinic.medicine.vo;

import java.math.BigDecimal;
import java.util.List;

public record MedicineListItemVO(
        Long id,
        String name,
        String genericName,
        String dosageForm,
        String specification,
        String baseUnit,
        String packageUnit,
        String manufacturer,
        BigDecimal purchasePrice,
        BigDecimal stockThreshold,
        BigDecimal stockThresholdInPackages,
        String status,
        List<String> barcodes
) {
}
