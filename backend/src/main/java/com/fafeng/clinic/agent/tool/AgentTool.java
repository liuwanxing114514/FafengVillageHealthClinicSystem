package com.fafeng.clinic.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;

public interface AgentTool {

    String name();

    String description();

    AgentToolResult execute(JsonNode args);
}
