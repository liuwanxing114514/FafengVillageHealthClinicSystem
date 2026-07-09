package com.fafeng.clinic.medicine.vo;

import java.util.List;

public record PageVO<T>(
        List<T> records,
        long total,
        long page,
        long size
) {
}
