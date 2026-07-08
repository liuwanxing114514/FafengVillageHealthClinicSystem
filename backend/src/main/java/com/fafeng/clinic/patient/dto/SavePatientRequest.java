package com.fafeng.clinic.patient.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record SavePatientRequest(
        @NotBlank @Size(max = 64) String name,
        @NotBlank @Size(max = 8) String gender,
        @Size(max = 18) String idCard,
        LocalDate birthDate,
        @Min(0) @Max(150) Integer age,
        @Size(max = 20) String phone,
        @Size(max = 256) String address,
        String remark
) {
}
