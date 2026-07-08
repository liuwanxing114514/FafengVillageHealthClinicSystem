package com.fafeng.clinic.clinic.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record PrescriptionDetailVO(
        Long id,
        Long patientId,
        String patientName,
        Long visitId,
        LocalDate prescriptionDate,
        String diagnosis,
        String status,
        List<PrescriptionItemVO> items,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
