package com.fafeng.clinic.agent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fafeng.clinic.agent.entity.AgentConversation;
import com.fafeng.clinic.agent.entity.AgentMessage;
import com.fafeng.clinic.agent.mapper.AgentConversationMapper;
import com.fafeng.clinic.agent.mapper.AgentMessageMapper;
import com.fafeng.clinic.agent.vo.AgentConversationVO;
import com.fafeng.clinic.agent.vo.AgentMessageVO;
import com.fafeng.clinic.agent.vo.AgentReferenceVO;
import com.fafeng.clinic.agent.vo.AgentToolCallVO;
import com.fafeng.clinic.agent.vo.PendingActionVO;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class AgentConversationService {

    private static final int TITLE_MAX = 40;
    private static final TypeReference<List<AgentToolCallVO>> TOOL_CALLS_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<AgentReferenceVO>> REFERENCES_TYPE = new TypeReference<>() {
    };
    private static final TypeReference<List<PendingActionVO>> PENDING_TYPE = new TypeReference<>() {
    };

    private final AgentConversationMapper conversationMapper;
    private final AgentMessageMapper messageMapper;
    private final ObjectMapper objectMapper;

    public AgentConversationService(AgentConversationMapper conversationMapper,
                                    AgentMessageMapper messageMapper,
                                    ObjectMapper objectMapper) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.objectMapper = objectMapper;
    }

    public String createConversation() {
        String id = UUID.randomUUID().toString().replace("-", "");
        OffsetDateTime now = OffsetDateTime.now();
        AgentConversation conversation = new AgentConversation();
        conversation.setId(id);
        conversation.setTitle("新对话");
        conversation.setMessageCount(0);
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);
        conversationMapper.insert(conversation);
        return id;
    }

    public AgentConversation requireConversation(String conversationId) {
        AgentConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "会话不存在");
        }
        return conversation;
    }

    public String resolveConversationId(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return createConversation();
        }
        String id = sessionId.trim();
        if (conversationMapper.selectById(id) == null) {
            OffsetDateTime now = OffsetDateTime.now();
            AgentConversation conversation = new AgentConversation();
            conversation.setId(id);
            conversation.setTitle("新对话");
            conversation.setMessageCount(0);
            conversation.setCreatedAt(now);
            conversation.setUpdatedAt(now);
            conversationMapper.insert(conversation);
        }
        return id;
    }

    public List<AgentConversationVO> listConversations(int limit) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        return conversationMapper.selectList(new LambdaQueryWrapper<AgentConversation>()
                        .orderByDesc(AgentConversation::getUpdatedAt)
                        .last("LIMIT " + safeLimit))
                .stream()
                .map(this::toConversationVO)
                .toList();
    }

    public List<AgentMessageVO> listMessages(String conversationId) {
        requireConversation(conversationId);
        return messageMapper.selectList(new LambdaQueryWrapper<AgentMessage>()
                        .eq(AgentMessage::getConversationId, conversationId)
                        .orderByAsc(AgentMessage::getId))
                .stream()
                .map(this::toMessageVO)
                .toList();
    }

    public List<Message> loadRecentHistory(String conversationId, int maxMessages) {
        requireConversation(conversationId);
        List<AgentMessage> rows = messageMapper.selectList(new LambdaQueryWrapper<AgentMessage>()
                .eq(AgentMessage::getConversationId, conversationId)
                .orderByDesc(AgentMessage::getId)
                .last("LIMIT " + maxMessages));
        List<Message> history = new ArrayList<>(rows.size());
        for (int i = rows.size() - 1; i >= 0; i--) {
            AgentMessage row = rows.get(i);
            if ("user".equals(row.getRole())) {
                history.add(new UserMessage(row.getContent()));
            } else if ("assistant".equals(row.getRole())) {
                history.add(new AssistantMessage(row.getContent()));
            }
        }
        return history;
    }

    @Transactional
    public void appendMessages(String conversationId,
                               String userContent,
                               String assistantContent,
                               List<AgentToolCallVO> toolCalls,
                               List<AgentReferenceVO> references,
                               List<PendingActionVO> pendingActions,
                               String titleIfNew) {
        OffsetDateTime now = OffsetDateTime.now();
        AgentConversation conversation = requireConversation(conversationId);

        AgentMessage userMsg = new AgentMessage();
        userMsg.setConversationId(conversationId);
        userMsg.setRole("user");
        userMsg.setContent(userContent);
        userMsg.setCreatedAt(now);
        messageMapper.insert(userMsg);

        AgentMessage assistantMsg = new AgentMessage();
        assistantMsg.setConversationId(conversationId);
        assistantMsg.setRole("assistant");
        assistantMsg.setContent(assistantContent);
        assistantMsg.setToolCallsJson(writeJson(toolCalls));
        assistantMsg.setReferencesJson(writeJson(references));
        assistantMsg.setPendingActionsJson(writeJson(pendingActions));
        assistantMsg.setCreatedAt(now);
        messageMapper.insert(assistantMsg);

        int newCount = (conversation.getMessageCount() == null ? 0 : conversation.getMessageCount()) + 2;
        conversation.setMessageCount(newCount);
        conversation.setUpdatedAt(now);
        if (newCount == 2 && titleIfNew != null && !titleIfNew.isBlank()) {
            conversation.setTitle(truncateTitle(titleIfNew));
        }
        conversationMapper.updateById(conversation);
    }

    @Transactional
    public void deleteConversation(String conversationId) {
        requireConversation(conversationId);
        messageMapper.delete(new LambdaQueryWrapper<AgentMessage>()
                .eq(AgentMessage::getConversationId, conversationId));
        conversationMapper.deleteById(conversationId);
    }

    private AgentConversationVO toConversationVO(AgentConversation c) {
        return new AgentConversationVO(
                c.getId(),
                c.getTitle(),
                c.getMessageCount() == null ? 0 : c.getMessageCount(),
                c.getCreatedAt(),
                c.getUpdatedAt());
    }

    private AgentMessageVO toMessageVO(AgentMessage m) {
        return new AgentMessageVO(
                m.getId(),
                m.getRole(),
                m.getContent(),
                readJson(m.getToolCallsJson(), TOOL_CALLS_TYPE, List.of()),
                readJson(m.getReferencesJson(), REFERENCES_TYPE, List.of()),
                readJson(m.getPendingActionsJson(), PENDING_TYPE, List.of()),
                m.getCreatedAt());
    }

    private String truncateTitle(String text) {
        String trimmed = text.trim().replaceAll("\\s+", " ");
        if (trimmed.length() <= TITLE_MAX) {
            return trimmed;
        }
        return trimmed.substring(0, TITLE_MAX) + "…";
    }

    private String writeJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return null;
        }
    }

    private <T> T readJson(String json, TypeReference<T> type, T fallback) {
        if (json == null || json.isBlank()) {
            return fallback;
        }
        try {
            return objectMapper.readValue(json, type);
        } catch (Exception ex) {
            return fallback;
        }
    }
}
