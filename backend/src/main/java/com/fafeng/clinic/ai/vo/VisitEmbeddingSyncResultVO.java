package com.fafeng.clinic.ai.vo;

public record VisitEmbeddingSyncResultVO(
        String mode,
        long total,
        long synced,
        long skipped,
        long failed,
        long durationMs
) {
}
