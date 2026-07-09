package com.fafeng.clinic.clinic.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record SavePrescriptionRequest(
        @NotNull Long patientId,
        @NotNull Long visitId,
        LocalDate prescriptionDate,
        String diagnosis,
        @NotEmpty @Valid List<SavePrescriptionItemRequest> items
) {
}
