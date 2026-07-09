package com.fafeng.clinic.agent.vo;

import java.util.List;

public record AgentChatResponseVO(
        String sessionId,
        String answer,
        List<AgentToolCallVO> toolCalls,
        List<PendingActionVO> pendingActions
) {
}
