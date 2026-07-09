package com.fafeng.clinic.clinic.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record SaveVisitRequest(
        @NotNull Long patientId,
        OffsetDateTime visitTime,
        String chiefComplaint,
        String presentIllness,
        String pastHistory,
        BigDecimal temperature,
        @Size(max = 16) String bloodPressure,
        BigDecimal spo2,
        BigDecimal etco2,
        Integer heartRate,
        @Size(max = 64) String pulse,
        String allergyHistory,
        String diagnosis,
        String treatment,
        String remark,
        @DecimalMin("0") BigDecimal amountDue,
        @DecimalMin("0") BigDecimal amountPaid
) {
}
