package com.fafeng.clinic.ai.vo;

import java.time.OffsetDateTime;

public record VisitEmbeddingStatusVO(
        boolean enabled,
        String provider,
        String model,
        int dimensions,
        boolean configured,
        long activeVisitCount,
        long syncedCount,
        long pendingCount,
        OffsetDateTime latestSyncedAt
) {
}
