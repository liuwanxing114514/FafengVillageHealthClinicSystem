package com.fafeng.clinic.patient.vo;

import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PatientListItemVO(
        Long id,
        String name,
        String gender,
        String idCard,
        LocalDate birthDate,
        Integer age,
        Boolean ageManual,
        String phone,
        String address,
        OffsetDateTime updatedAt
) {
}
