package com.fafeng.clinic.agent.vo;

public record AgentToolCallVO(
        String toolName,
        String argsSummary,
        String displayArgsSummary,
        String resultSummary,
        String dataJson,
        long durationMs,
        boolean success
) {
}
