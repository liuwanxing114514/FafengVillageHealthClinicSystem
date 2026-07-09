package com.fafeng.clinic.ai.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record VisitDraftPayload(
        Long patientId,
        Long visitId,
        String inputText,
        String chiefComplaint,
        String presentIllness,
        String pastHistory,
        String allergyHistory,
        String diagnosis,
        String treatment,
        String remark
) {
}
