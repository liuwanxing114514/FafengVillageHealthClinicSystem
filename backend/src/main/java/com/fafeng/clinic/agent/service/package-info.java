/**
 * Agent 编排与单次对话上下文。
 *
 * <ul>
 *   <li>{@link com.fafeng.clinic.agent.service.AgentOrchestrator} — 一次 chat 的主流程</li>
 *   <li>{@link com.fafeng.clinic.agent.service.AgentToolCallContext} — ThreadLocal，记录本轮调用了哪些工具（供日志与 references）</li>
 *   <li>{@link com.fafeng.clinic.agent.service.AgentReferenceExtractor} — 从工具 JSON 结果提取可跳转的患者/病历 ID</li>
 *   <li>{@link com.fafeng.clinic.agent.service.AgentExecutionLogService} — 持久化工具调用审计</li>
 * </ul>
 */
package com.fafeng.clinic.agent.service;
