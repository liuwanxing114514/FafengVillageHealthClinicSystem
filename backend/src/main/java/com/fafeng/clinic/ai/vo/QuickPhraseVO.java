package com.fafeng.clinic.ai.vo;

import java.time.OffsetDateTime;

public record QuickPhraseVO(
        Long id,
        String fieldKey,
        String fieldLabel,
        String content,
        String source,
        int useCount,
        OffsetDateTime lastUsedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
