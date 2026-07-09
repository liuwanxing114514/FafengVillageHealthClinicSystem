package com.fafeng.clinic.patient.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public record PatientDetailVO(
        Long id,
        String name,
        String gender,
        String idCard,
        LocalDate birthDate,
        Integer age,
        Boolean ageManual,
        String phone,
        String address,
        String remark,
        BigDecimal totalArrears,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
