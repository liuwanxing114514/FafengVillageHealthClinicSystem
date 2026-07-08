package com.fafeng.clinic.medicine.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveConversionRequest(
        @NotBlank @Size(max = 16) String fromUnit,
        @NotBlank @Size(max = 16) String toUnit,
        @NotNull @Min(1) Integer factor
) {
}
