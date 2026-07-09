package com.fafeng.clinic.importexcel.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record MedicineImportRowVO(
        int rowNumber,
        boolean valid,
        List<String> errors,
        String name,
        String genericName,
        String dosageForm,
        String specification,
        String baseUnit,
        String packageUnit,
        String conversionText,
        String manufacturer,
        String barcode,
        BigDecimal purchasePrice,
        BigDecimal stockThreshold,
        String batchNo,
        LocalDate expiryDate,
        BigDecimal initialStock,
        String remark
) {
}
