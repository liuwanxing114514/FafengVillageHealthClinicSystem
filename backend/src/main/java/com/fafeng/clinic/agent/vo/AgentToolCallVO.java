package com.fafeng.clinic.agent.vo;

public record AgentToolCallVO(
        String toolName,
        String argsSummary,
        String resultSummary,
        long durationMs,
        boolean success
) {
}
