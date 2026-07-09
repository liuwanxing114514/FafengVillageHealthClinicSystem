package com.fafeng.clinic.clinic.vo;

import java.math.BigDecimal;

public record VisitFeeSummaryVO(
        BigDecimal suggestedAmountDue,
        BigDecimal referencePurchaseCost
) {
}
