package com.fafeng.clinic.importexcel.vo;

import java.util.List;

public record MedicineImportPreviewVO(
        String previewId,
        int totalRows,
        int validCount,
        int errorCount,
        boolean canConfirm,
        List<MedicineImportRowVO> rows
) {
}
