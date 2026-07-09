package com.fafeng.clinic.agent.service;

import com.fafeng.clinic.agent.dto.AgentChatRequest;
import com.fafeng.clinic.agent.tool.ClinicAgentTools;
import com.fafeng.clinic.agent.vo.AgentChatResponseVO;
import com.fafeng.clinic.agent.vo.AgentToolCallVO;
import com.fafeng.clinic.agent.vo.PendingActionVO;
import com.fafeng.clinic.ai.client.AiChatClient;
import com.fafeng.clinic.ai.config.ClinicAiProperties;
import com.fafeng.clinic.ai.entity.AiDraft;
import com.fafeng.clinic.ai.util.Desensitizer;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Agent 编排：用户消息脱敏 → Spring AI {@code ChatClient} + {@code @Tool} → 结果摘要与待确认卡片。
 */
@Service
@RequiredArgsConstructor
public class AgentOrchestrator {

    private final ClinicAiProperties properties;
    private final AiChatClient aiChatClient;
    private final ClinicAgentTools clinicAgentTools;
    private final AgentToolCallContext callContext;
    private final AgentExecutionLogService executionLogService;

    public AgentChatResponseVO chat(AgentChatRequest request) {
        if (!properties.isEnabled()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 功能未启用");
        }
        if (!aiChatClient.isConfigured()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 服务不可用，请检查配置");
        }

        String sessionId = request.sessionId() == null || request.sessionId().isBlank()
                ? UUID.randomUUID().toString().replace("-", "")
                : request.sessionId().trim();
        String userMessage = request.message().trim();
        String desensitizedMessage = Desensitizer.desensitizeText(userMessage, Desensitizer.PatientContext.empty());

        callContext.activate();
        try {
            String answer = aiChatClient.chatWithTools(
                    buildSystemPrompt(),
                    desensitizedMessage,
                    clinicAgentTools);

            List<AgentToolCallVO> toolCalls = new ArrayList<>();
            List<PendingActionVO> pendingActions = new ArrayList<>();
            for (AgentToolCallContext.ToolCallRecord record : callContext.getRecords()) {
                executionLogService.log(
                        sessionId,
                        record.toolName(),
                        record.argsSummary(),
                        record.resultSummary(),
                        record.durationMs());
                toolCalls.add(new AgentToolCallVO(
                        record.toolName(),
                        record.argsSummary(),
                        record.resultSummary(),
                        record.durationMs(),
                        record.success()));
                if (record.pendingDraftId() != null) {
                    pendingActions.add(new PendingActionVO(
                            record.pendingDraftId(),
                            AiDraft.TYPE_OUTBOUND,
                            record.resultSummary()));
                }
            }

            if (answer == null || answer.isBlank()) {
                answer = summarizeFromToolCalls(toolCalls, userMessage);
            }
            if (answer.isBlank()) {
                answer = "抱歉，暂时无法完成该查询，请换个方式提问或手动操作。";
            }
            return new AgentChatResponseVO(sessionId, answer, toolCalls, pendingActions);
        } finally {
            callContext.clear();
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
