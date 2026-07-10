package com.fafeng.clinic.agent.vo;

/**
 * Agent 回复中的可跳转业务引用（患者、病历等）。
 *
 * @param refType {@link #TYPE_PATIENT} 或 {@link #TYPE_VISIT}，前端映射路由
 * @param refId   业务主键（patient.id 或 visit.id）
 * @param label   按钮文案，如「查看患者 李**」
 * @param hint    副文案，如「男 · 60岁」
 */
public record AgentReferenceVO(
        String refType,
        Long refId,
        String label,
        String hint
) {
    public static final String TYPE_PATIENT = "patient";
    public static final String TYPE_VISIT = "visit";
    public static final String TYPE_MEDICINE = "medicine";
}
