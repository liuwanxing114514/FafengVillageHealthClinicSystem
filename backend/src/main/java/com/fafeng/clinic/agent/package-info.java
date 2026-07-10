/**
 * AI 助手（Agent）：自然语言 → 大模型 → 受控工具 → 诊所数据。
 *
 * <h2>一次对话的流程</h2>
 * <ol>
 *   <li>前端 {@code POST /api/agent/chat} 发送用户问题</li>
 *   <li>{@link com.fafeng.clinic.agent.service.AgentOrchestrator} 脱敏后调用
 *       {@link com.fafeng.clinic.ai.client.AiChatClient#chatWithTools}</li>
 *   <li>Spring AI 把 {@link com.fafeng.clinic.agent.tool.ClinicAgentTools} 中的 {@code @Tool} 暴露给模型</li>
 *   <li>模型若决定查数据，会回调工具 → {@link com.fafeng.clinic.agent.tool.AgentToolRegistry} → 各 *Tool 实现</li>
 *   <li>编排层汇总 {@code answer}、{@code toolCalls}、{@code references}（可跳转患者/病历）、{@code pendingActions}（出库草稿）</li>
 * </ol>
 *
 * <h2>包内职责</h2>
 * <ul>
 *   <li>{@code tool} — 原子能力：查患者、查库存、生成出库草稿等</li>
 *   <li>{@code service} — 编排、单次对话内的工具调用上下文、执行日志</li>
 *   <li>{@code controller} — {@code /api/agent/chat}、{@code /api/agent/logs}</li>
 * </ul>
 *
 * <p>注意：Agent 不会自动「看见」全库数据；必须模型主动调用 {@code searchPatient} 等工具才有查询结果。
 */
package com.fafeng.clinic.agent;
