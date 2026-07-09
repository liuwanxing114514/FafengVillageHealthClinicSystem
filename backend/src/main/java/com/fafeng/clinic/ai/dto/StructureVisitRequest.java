package com.fafeng.clinic.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record StructureVisitRequest(
        @NotBlank @Size(max = 8000) String text,
        Long patientId,
        Long visitId
) {
}
