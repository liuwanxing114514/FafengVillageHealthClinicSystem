package com.fafeng.clinic.agent.vo;

import java.time.OffsetDateTime;

public record AgentConversationVO(
        String id,
        String title,
        int messageCount,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
