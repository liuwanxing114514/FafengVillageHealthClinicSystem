package com.fafeng.clinic.importexcel.dto;

import jakarta.validation.constraints.NotBlank;

public record MedicineImportConfirmRequest(
        @NotBlank String previewId
) {
}
