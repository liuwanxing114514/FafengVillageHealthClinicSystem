package com.fafeng.clinic.ai.dto;

import jakarta.validation.constraints.NotNull;

public record ApproveVisitDraftRequest(
        @NotNull Long patientId
) {
}
