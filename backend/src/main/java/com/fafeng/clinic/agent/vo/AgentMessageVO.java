package com.fafeng.clinic.agent.vo;

import java.time.OffsetDateTime;
import java.util.List;

public record AgentMessageVO(
        Long id,
        String role,
        String content,
        List<AgentToolCallVO> toolCalls,
        List<AgentReferenceVO> references,
        List<PendingActionVO> pendingActions,
        OffsetDateTime createdAt
) {
}
