package com.fafeng.clinic.clinic.dto;

import java.time.LocalDate;

public record VisitSearchQuery(
        String keyword,
        LocalDate dateFrom,
        LocalDate dateTo,
        Boolean arrearsOnly
) {
}
