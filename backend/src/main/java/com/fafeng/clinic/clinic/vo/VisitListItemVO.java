package com.fafeng.clinic.clinic.vo;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record VisitListItemVO(
        Long id,
        Long patientId,
        String patientName,
        String patientGender,
        OffsetDateTime visitTime,
        String chiefComplaint,
        String diagnosis,
        BigDecimal amountDue,
        BigDecimal amountPaid,
        BigDecimal balance,
        String status
) {
}
