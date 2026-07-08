package com.fafeng.clinic.ai.vo;

import java.time.OffsetDateTime;

public record AiDraftVO(
        Long id,
        String draftType,
        String status,
        String payload,
        String source,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
