package com.fafeng.clinic.agent.controller;

import com.fafeng.clinic.agent.dto.AgentChatRequest;
import com.fafeng.clinic.agent.service.AgentConversationService;
import com.fafeng.clinic.agent.service.AgentExecutionLogService;
import com.fafeng.clinic.agent.service.AgentOrchestrator;
import com.fafeng.clinic.agent.vo.AgentChatResponseVO;
import com.fafeng.clinic.agent.vo.AgentConversationVO;
import com.fafeng.clinic.agent.vo.AgentExecutionLogVO;
import com.fafeng.clinic.agent.vo.AgentMessageVO;
import com.fafeng.clinic.common.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentOrchestrator orchestrator;
    private final AgentExecutionLogService executionLogService;
    private final AgentConversationService conversationService;

    @PostMapping("/chat")
    public Result<AgentChatResponseVO> chat(@Valid @RequestBody AgentChatRequest request) {
        return Result.ok(orchestrator.chat(request));
    }

    @GetMapping("/conversations")
    public Result<List<AgentConversationVO>> conversations(
            @RequestParam(defaultValue = "50") int limit) {
        return Result.ok(conversationService.listConversations(limit));
    }

    @GetMapping("/conversations/{id}/messages")
    public Result<List<AgentMessageVO>> messages(@PathVariable String id) {
        return Result.ok(conversationService.listMessages(id));
    }

    @DeleteMapping("/conversations/{id}")
    public Result<Void> deleteConversation(@PathVariable String id) {
        conversationService.deleteConversation(id);
        return Result.ok(null);
    }

    @GetMapping("/logs")
    public Result<List<AgentExecutionLogVO>> logs(
            @RequestParam(required = false) String sessionId,
            @RequestParam(defaultValue = "20") int limit) {
        if (sessionId != null && !sessionId.isBlank()) {
            return Result.ok(executionLogService.listBySession(sessionId.trim()));
        }
        return Result.ok(executionLogService.listRecent(limit));
    }
}
