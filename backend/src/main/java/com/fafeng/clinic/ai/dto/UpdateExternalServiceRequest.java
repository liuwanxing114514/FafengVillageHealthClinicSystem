package com.fafeng.clinic.ai.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateExternalServiceRequest(
        @NotNull Boolean enabled,
        String endpointUrl) {
}
