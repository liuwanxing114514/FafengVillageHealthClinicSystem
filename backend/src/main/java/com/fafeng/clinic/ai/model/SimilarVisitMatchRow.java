package com.fafeng.clinic.ai.model;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@NoArgsConstructor
public class SimilarVisitMatchRow {

    private Long visitId;
    private String textSummary;
    private Double similarity;
    private OffsetDateTime visitTime;
}
