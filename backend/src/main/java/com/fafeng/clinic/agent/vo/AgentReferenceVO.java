package com.fafeng.clinic.agent.vo;

/**
 * Agent 回复中的可跳转业务引用（患者、病历等）。
 */
public record AgentReferenceVO(
        String refType,
        Long refId,
        String label,
        String hint
) {
    public static final String TYPE_PATIENT = "patient";
    public static final String TYPE_VISIT = "visit";
}
