package com.fafeng.clinic.agent.service;

import com.fafeng.clinic.agent.tool.AgentToolResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 单次 Agent 对话内的工具调用记录，供编排层写日志与待确认卡片。
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
                result.pendingDraftId()));
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
            Long pendingDraftId
    ) {
    }
}
