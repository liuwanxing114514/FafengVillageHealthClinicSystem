/**
 * Agent 工具层：每个 Tool 对应一项受控业务能力（只读查询或写草稿，不直接改库存/病历）。
 *
 * <p>{@link com.fafeng.clinic.agent.tool.ClinicAgentTools} 用 Spring AI {@code @Tool} 注解暴露给大模型；
 * 内部通过 {@link com.fafeng.clinic.agent.tool.AgentToolRegistry} 分发到本包各 {@link com.fafeng.clinic.agent.tool.AgentTool} 实现。
 *
 * <p>工具名常量见 {@link com.fafeng.clinic.agent.tool.AgentToolName}（与前端 {@code agentLabels.ts} 中文映射对应）。
 */
package com.fafeng.clinic.agent.tool;
