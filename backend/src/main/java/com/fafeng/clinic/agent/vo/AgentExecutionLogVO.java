package com.fafeng.clinic.agent.vo;

import java.time.OffsetDateTime;

public record AgentExecutionLogVO(
        Long id,
        String sessionId,
        String toolName,
        String argsSummary,
        String resultSummary,
        Integer durationMs,
        OffsetDateTime createdAt
) {
}
