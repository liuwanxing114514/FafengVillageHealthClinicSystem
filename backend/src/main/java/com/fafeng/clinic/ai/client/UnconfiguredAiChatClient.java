package com.fafeng.clinic.ai.client;

import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;

/**
 * AI 未启用或未配置时的占位客户端，与 noop 行为一致。
 */
public class UnconfiguredAiChatClient implements AiChatClient {

    @Override
    public boolean isConfigured() {
        return false;
    }

    @Override
    public String chatCompletion(String systemPrompt, String userMessage) {
        throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "DeepSeek 未配置");
    }

    @Override
    public String chatWithTools(String systemPrompt, String userMessage, Object toolSource) {
        throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "DeepSeek 未配置");
    }
}
