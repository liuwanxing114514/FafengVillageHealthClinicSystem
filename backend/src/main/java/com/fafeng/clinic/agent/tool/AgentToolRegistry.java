package com.fafeng.clinic.agent.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class AgentToolRegistry {

    private final Map<String, AgentTool> tools;

    public AgentToolRegistry(List<AgentTool> toolList) {
        Map<String, AgentTool> map = new LinkedHashMap<>();
        for (AgentTool tool : toolList) {
            map.put(tool.name(), tool);
        }
        this.tools = Map.copyOf(map);
    }

    public AgentToolResult execute(String toolName, JsonNode args) {
        if (!AgentToolName.isRegistered(toolName)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "未注册的工具：" + toolName);
        }
        AgentTool tool = tools.get(toolName);
        if (tool == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "工具不可用：" + toolName);
        }
        return tool.execute(args == null ? args : args);
    }

    public String buildToolCatalog() {
        StringBuilder builder = new StringBuilder();
        for (AgentTool tool : tools.values()) {
            builder.append("- ").append(tool.name()).append(": ").append(tool.description()).append('\n');
        }
        return builder.toString();
    }
}
