package com.fafeng.clinic.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record StructureVisitRequest(
        @NotBlank String text,
        Long patientId
) {
}
