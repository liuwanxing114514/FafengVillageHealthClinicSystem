package com.fafeng.clinic.medicine.vo;

public record ConversionVO(
        Long id,
        String fromUnit,
        String toUnit,
        Integer factor
) {
}
