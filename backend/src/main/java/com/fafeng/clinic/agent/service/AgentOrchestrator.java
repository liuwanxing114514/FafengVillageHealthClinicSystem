package com.fafeng.clinic.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fafeng.clinic.agent.dto.AgentChatRequest;
import com.fafeng.clinic.agent.tool.AgentToolRegistry;
import com.fafeng.clinic.agent.tool.AgentToolResult;
import com.fafeng.clinic.agent.vo.AgentChatResponseVO;
import com.fafeng.clinic.agent.vo.AgentToolCallVO;
import com.fafeng.clinic.agent.vo.PendingActionVO;
import com.fafeng.clinic.ai.config.ClinicAiProperties;
import com.fafeng.clinic.ai.entity.AiDraft;
import com.fafeng.clinic.ai.provider.AiProvider;
import com.fafeng.clinic.ai.service.AiJsonParser;
import com.fafeng.clinic.ai.util.Desensitizer;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AgentOrchestrator {

    private final ClinicAiProperties properties;
    private final AiProvider activeAiProvider;
    private final AgentToolRegistry toolRegistry;
    private final AgentExecutionLogService executionLogService;
    private final ObjectMapper objectMapper;

    public AgentOrchestrator(ClinicAiProperties properties,
                             AiProvider activeAiProvider,
                             AgentToolRegistry toolRegistry,
                             AgentExecutionLogService executionLogService,
                             ObjectMapper objectMapper) {
        this.properties = properties;
        this.activeAiProvider = activeAiProvider;
        this.toolRegistry = toolRegistry;
        this.executionLogService = executionLogService;
        this.objectMapper = objectMapper;
    }

    public AgentChatResponseVO chat(AgentChatRequest request) {
        if (!properties.isEnabled()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 功能未启用");
        }
        if (!activeAiProvider.isAvailable()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 服务不可用，请检查配置");
        }

        String sessionId = request.sessionId() == null || request.sessionId().isBlank()
                ? UUID.randomUUID().toString().replace("-", "")
                : request.sessionId().trim();
        String userMessage = request.message().trim();
        String desensitizedMessage = Desensitizer.desensitizeText(userMessage, Desensitizer.PatientContext.empty());

        List<AgentToolCallVO> toolCalls = new ArrayList<>();
        List<PendingActionVO> pendingActions = new ArrayList<>();
        StringBuilder context = new StringBuilder();
        context.append("用户问题：").append(desensitizedMessage).append('\n');

        String answer = null;
        int maxRounds = properties.getAgentMaxRounds();

        for (int round = 0; round < maxRounds; round++) {
            String systemPrompt = buildSystemPrompt();
            String userPrompt = context + "\n请决定下一步：调用工具或给出最终回答。";
            String llmResponse = activeAiProvider.chatCompletion(systemPrompt, userPrompt);
            JsonNode plan = AiJsonParser.parseJsonContent(objectMapper, llmResponse);

            String action = plan.path("action").asText("");
            if ("final_answer".equals(action) || plan.has("answer")) {
                answer = plan.path("answer").asText("");
                if (answer.isBlank()) {
                    answer = llmResponse;
                }
                break;
            }

            if (!"call_tool".equals(action)) {
                answer = plan.path("answer").asText(llmResponse);
                break;
            }

            String toolName = plan.path("tool").asText("");
            JsonNode args = plan.path("args");
            String argsSummary = summarizeArgs(args);

            long start = System.currentTimeMillis();
            AgentToolResult result;
            try {
                result = toolRegistry.execute(toolName, args);
            } catch (BusinessException ex) {
                result = AgentToolResult.fail(ex.getMessage());
            }
            long duration = System.currentTimeMillis() - start;

            executionLogService.log(sessionId, toolName, argsSummary, result.summary(), duration);
            toolCalls.add(new AgentToolCallVO(toolName, argsSummary, result.summary(), duration, result.success()));

            if (result.pendingDraftId() != null) {
                pendingActions.add(new PendingActionVO(
                        result.pendingDraftId(),
                        AiDraft.TYPE_OUTBOUND,
                        result.summary()));
            }

            context.append("\n工具 ").append(toolName).append(" 结果：").append(result.summary());
            if (result.data() != null) {
                try {
                    context.append("\n数据：").append(objectMapper.writeValueAsString(result.data()));
                } catch (Exception ignored) {
                    // skip serialization error in context
                }
            }

            if (round == maxRounds - 1) {
                answer = summarizeFromToolCalls(toolCalls, userMessage);
            }
        }

        if (answer == null || answer.isBlank()) {
            answer = "抱歉，暂时无法完成该查询，请换个方式提问或手动操作。";
        }

        return new AgentChatResponseVO(sessionId, answer, toolCalls, pendingActions);
    }

    private String buildSystemPrompt() {
        return properties.getAgentSystemPrompt().replace("{{TOOLS}}", toolRegistry.buildToolCatalog());
    }

    private String summarizeArgs(JsonNode args) {
        if (args == null || args.isNull() || args.isEmpty()) {
            return "{}";
        }
        try {
            String json = objectMapper.writeValueAsString(args);
            return json.length() > 500 ? json.substring(0, 500) : json;
        } catch (Exception ex) {
            return args.toString();
        }
    }

    private String summarizeFromToolCalls(List<AgentToolCallVO> toolCalls, String question) {
        if (toolCalls.isEmpty()) {
            return "未能处理您的问题：" + question;
        }
        StringBuilder builder = new StringBuilder();
        for (AgentToolCallVO call : toolCalls) {
            builder.append(call.resultSummary()).append(' ');
        }
        return builder.toString().trim();
    }
}
