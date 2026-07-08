package com.fafeng.clinic.patient.dto;

public record PatientSearchQuery(
        String keyword,
        String name,
        String phone,
        String idCard,
        String address,
        String gender,
        String remark,
        Integer ageMin,
        Integer ageMax
) {
}
