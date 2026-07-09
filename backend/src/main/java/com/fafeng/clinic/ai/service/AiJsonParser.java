package com.fafeng.clinic.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;

public final class AiJsonParser {

    private AiJsonParser() {
    }

    public static JsonNode parseJsonContent(ObjectMapper objectMapper, String content) {
        String normalized = stripMarkdownFence(content);
        try {
            return objectMapper.readTree(normalized);
        } catch (JsonProcessingException ex) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 返回格式无效，请重试或手动录入");
        }
    }

    private static String stripMarkdownFence(String content) {
        if (content == null) {
            return "{}";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }
}
