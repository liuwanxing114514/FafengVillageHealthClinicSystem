package com.fafeng.clinic.clinic.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record VisitDetailVO(
        Long id,
        Long patientId,
        String patientName,
        OffsetDateTime visitTime,
        String chiefComplaint,
        String presentIllness,
        String pastHistory,
        BigDecimal temperature,
        String bloodPressure,
        BigDecimal spo2,
        BigDecimal etco2,
        Integer heartRate,
        String pulse,
        String allergyHistory,
        String diagnosis,
        String treatment,
        String remark,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
