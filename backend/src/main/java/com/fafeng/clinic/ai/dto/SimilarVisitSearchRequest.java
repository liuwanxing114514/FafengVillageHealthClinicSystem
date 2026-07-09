package com.fafeng.clinic.ai.dto;

public record SimilarVisitSearchRequest(
        String chiefComplaint,
        String presentIllness,
        String diagnosis,
        Long patientId,
        Long excludeVisitId
) {
}
