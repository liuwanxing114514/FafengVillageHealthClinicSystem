package com.fafeng.clinic.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;

public record AgentToolResult(
        boolean success,
        JsonNode data,
        String summary,
        Long pendingDraftId
) {
    public static AgentToolResult ok(JsonNode data, String summary) {
        return new AgentToolResult(true, data, summary, null);
    }

    public static AgentToolResult okWithDraft(JsonNode data, String summary, Long draftId) {
        return new AgentToolResult(true, data, summary, draftId);
    }

    public static AgentToolResult fail(String summary) {
        return new AgentToolResult(false, null, summary, null);
    }
}
