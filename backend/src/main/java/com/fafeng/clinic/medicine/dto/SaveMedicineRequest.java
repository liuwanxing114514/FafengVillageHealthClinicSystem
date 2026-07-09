package com.fafeng.clinic.medicine.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SaveMedicineRequest(
        @NotBlank @Size(max = 128) String name,
        @Size(max = 128) String genericName,
        @Size(max = 32) String dosageForm,
        @Size(max = 64) String specification,
        @NotBlank @Size(max = 16) String baseUnit,
        @Size(max = 16) String packageUnit,
        @Size(max = 128) String manufacturer,
        @NotNull @DecimalMin("0") BigDecimal purchasePrice,
        @DecimalMin("0") BigDecimal suggestedRetailPrice,
        @DecimalMin("0") BigDecimal stockThreshold,
        String remark
) {
}
