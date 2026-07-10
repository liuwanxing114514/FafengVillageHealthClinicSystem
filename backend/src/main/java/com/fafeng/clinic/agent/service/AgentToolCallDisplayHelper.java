package com.fafeng.clinic.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fafeng.clinic.agent.tool.AgentToolName;
import com.fafeng.clinic.ai.util.Desensitizer;
import org.springframework.stereotype.Component;

@Component
public class AgentToolCallDisplayHelper {

    private final ObjectMapper objectMapper;

    public AgentToolCallDisplayHelper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String maskArgsForDisplay(String toolName, String argsSummary) {
        if (argsSummary == null || argsSummary.isBlank()) {
            return argsSummary;
        }
        if (!AgentToolName.SEARCH_PATIENT.equals(toolName)
                && !AgentToolName.SEARCH_PATIENT_VISIT.equals(toolName)) {
            return argsSummary;
        }
        try {
            JsonNode node = objectMapper.readTree(argsSummary);
            if (node instanceof ObjectNode objectNode && objectNode.has("keyword")) {
                JsonNode keyword = objectNode.get("keyword");
                if (keyword != null && keyword.isTextual()) {
                    objectNode.put("keyword", Desensitizer.maskName(keyword.asText()));
                }
                return objectMapper.writeValueAsString(objectNode);
            }
        } catch (Exception ignored) {
            // fall through
        }
        return argsSummary;
    }
}
