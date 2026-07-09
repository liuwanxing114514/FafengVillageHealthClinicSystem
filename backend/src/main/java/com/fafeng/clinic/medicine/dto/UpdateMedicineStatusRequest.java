package com.fafeng.clinic.medicine.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UpdateMedicineStatusRequest(
        @NotBlank
        @Pattern(regexp = "ACTIVE|INACTIVE")
        String status
) {
}
