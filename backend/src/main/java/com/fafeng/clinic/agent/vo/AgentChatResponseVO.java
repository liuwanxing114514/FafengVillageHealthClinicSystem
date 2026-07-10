package com.fafeng.clinic.agent.vo;

import java.util.List;

/**
 * Agent 聊天 API 响应。
 *
 * @param references 可跳转业务实体（患者、病历），由 {@link com.fafeng.clinic.agent.service.AgentReferenceExtractor} 生成，非大模型输出
 */
public record AgentChatResponseVO(
        String sessionId,
        String answer,
        List<AgentToolCallVO> toolCalls,
        List<PendingActionVO> pendingActions,
        List<AgentReferenceVO> references
) {
}
