package com.fafeng.clinic.clinic.vo;

import java.time.OffsetDateTime;

public record VisitListItemVO(
        Long id,
        Long patientId,
        OffsetDateTime visitTime,
        String chiefComplaint,
        String diagnosis,
        String status
) {
}
