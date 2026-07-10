package com.fafeng.clinic.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fafeng.clinic.agent.tool.AgentToolResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 单次 Agent 对话内的工具调用记录（ThreadLocal）。
 *
 * <p>{@link com.fafeng.clinic.agent.tool.ClinicAgentTools#executeTool} 每执行一个工具就 {@link #record} 一条；
 * 编排层据此写审计日志、组装 {@code toolCalls}，{@link AgentReferenceExtractor} 还可读 {@code data} JSON 做页面跳转。
 */
@Component
public class AgentToolCallContext {

    private final ThreadLocal<List<ToolCallRecord>> records = ThreadLocal.withInitial(ArrayList::new);

    public void activate() {
        records.set(new ArrayList<>());
    }

    public void clear() {
        records.remove();
    }

    public void record(String toolName, String argsSummary, AgentToolResult result, long durationMs) {
        records.get().add(new ToolCallRecord(
                toolName,
                argsSummary,
                result.summary(),
                durationMs,
                result.success(),
                result.pendingDraftId(),
                result.data()));
    }

    public List<ToolCallRecord> getRecords() {
        return List.copyOf(records.get());
    }

    public record ToolCallRecord(
            String toolName,
            String argsSummary,
            String resultSummary,
            long durationMs,
            boolean success,
            Long pendingDraftId,
            JsonNode data
    ) {
    }
}
