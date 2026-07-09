package com.fafeng.clinic.ai.vo;

import java.time.OffsetDateTime;

public record SimilarVisitVO(
        Long visitId,
        String textSummary,
        double similarity,
        OffsetDateTime visitTime
) {
}
