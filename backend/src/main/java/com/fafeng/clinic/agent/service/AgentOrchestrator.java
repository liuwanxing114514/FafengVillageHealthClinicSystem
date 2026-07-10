package com.fafeng.clinic.agent.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fafeng.clinic.agent.dto.AgentChatRequest;
import com.fafeng.clinic.agent.tool.ClinicAgentTools;
import com.fafeng.clinic.agent.vo.AgentChatResponseVO;
import com.fafeng.clinic.agent.vo.AgentReferenceVO;
import com.fafeng.clinic.agent.vo.AgentToolCallVO;
import com.fafeng.clinic.agent.vo.PendingActionVO;
import com.fafeng.clinic.ai.client.AiChatClient;
import com.fafeng.clinic.ai.config.ClinicAiProperties;
import com.fafeng.clinic.ai.config.ExternalServiceConfigService;
import com.fafeng.clinic.ai.entity.AiDraft;
import com.fafeng.clinic.ai.util.Desensitizer;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final ExternalServiceConfigService externalServiceConfigService;
    private final ClinicAiProperties properties;
    private final AiChatClient aiChatClient;
    private final ClinicAgentTools clinicAgentTools;
    private final AgentToolCallContext callContext;
    private final AgentExecutionLogService executionLogService;
    private final AgentReferenceExtractor referenceExtractor;
    private final AgentConversationService conversationService;
    private final AgentPrivacyCollector privacyCollector;
    private final AgentToolCallDisplayHelper displayHelper;
    private final ObjectMapper objectMapper;

    public AgentChatResponseVO chat(AgentChatRequest request) {
        if (!externalServiceConfigService.isChatEnabled()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 功能未启用");
        }
        if (!aiChatClient.isConfigured()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 服务不可用，请检查配置");
        }

        String conversationId = normalizeSessionId(request.sessionId());
        if (conversationId != null) {
            conversationService.resolveConversationId(conversationId);
        }

        String userMessage = request.message().trim();
        String desensitizedMessage = Desensitizer.desensitizeText(userMessage, Desensitizer.PatientContext.empty());

        int historyLimit = Math.max(properties.getAgentMaxRounds(), 1) * 2;
        List<Message> history = conversationId != null
                ? conversationService.loadRecentHistory(conversationId, historyLimit)
                : List.of();

        callContext.activate();
        privacyCollector.activate();
        try {
            String answer = aiChatClient.chatWithTools(
                    buildSystemPrompt(),
                    history,
                    desensitizedMessage,
                    clinicAgentTools);

            // 新会话须在写工具日志前分配 id（agent_execution_log.session_id NOT NULL）
            if (conversationId == null) {
                conversationId = conversationService.createConversation();
            }

            List<AgentToolCallContext.ToolCallRecord> records = callContext.getRecords();
            List<AgentToolCallVO> toolCalls = buildToolCalls(conversationId);
            List<PendingActionVO> pendingActions = buildPendingActions();

            if (answer == null || answer.isBlank()) {
                answer = summarizeFromToolCalls(toolCalls, userMessage);
            }
            if (answer.isBlank()) {
                answer = "抱歉，暂时无法完成该查询，请换个方式提问或手动操作。";
            }

            String safeAnswer = privacyCollector.desensitizeText(answer);
            List<AgentReferenceVO> references = referenceExtractor.extract(records);

            conversationService.appendMessages(
                    conversationId,
                    userMessage,
                    safeAnswer,
                    toolCalls,
                    references,
                    pendingActions,
                    userMessage);

            return new AgentChatResponseVO(conversationId, safeAnswer, toolCalls, pendingActions, references);
        } finally {
            callContext.clear();
            privacyCollector.clear();
        }
    }

    private String normalizeSessionId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }
        return sessionId.trim();
    }

    private List<AgentToolCallVO> buildToolCalls(String sessionId) {
        List<AgentToolCallVO> toolCalls = new ArrayList<>();
        for (AgentToolCallContext.ToolCallRecord record : callContext.getRecords()) {
            executionLogService.log(
                    sessionId,
                    record.toolName(),
                    record.argsSummary(),
                    record.resultSummary(),
                    record.durationMs());
            String displayArgs = displayHelper.maskArgsForDisplay(record.toolName(), record.argsSummary());
            String dataJson = serializeData(record.data());
            toolCalls.add(new AgentToolCallVO(
                    record.toolName(),
                    record.argsSummary(),
                    displayArgs,
                    record.resultSummary(),
                    dataJson,
                    record.durationMs(),
                    record.success()));
        }
        return toolCalls;
    }

    private List<PendingActionVO> buildPendingActions() {
        List<PendingActionVO> pendingActions = new ArrayList<>();
        for (AgentToolCallContext.ToolCallRecord record : callContext.getRecords()) {
            if (record.pendingDraftId() != null) {
                pendingActions.add(new PendingActionVO(
                        record.pendingDraftId(),
                        AiDraft.TYPE_OUTBOUND,
                        record.resultSummary()));
            }
        }
        return pendingActions;
    }

    private String serializeData(JsonNode data) {
        if (data == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception ex) {
            return null;
        }
    }

    private String buildSystemPrompt() {
        return properties.getAgentSystemPrompt();
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
