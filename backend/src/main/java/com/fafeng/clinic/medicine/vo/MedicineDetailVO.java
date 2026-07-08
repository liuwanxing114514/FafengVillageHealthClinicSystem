package com.fafeng.clinic.medicine.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record MedicineDetailVO(
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
        String pinyinAbbr,
        String remark,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        List<ConversionVO> conversions,
        List<BarcodeVO> barcodes
) {
}
