package com.fafeng.clinic.ai.provider;

import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class NoopAiProvider implements AiProvider {

    @Override
    public String name() {
        return "noop";
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String chatCompletion(String systemPrompt, String userMessage) {
        throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 功能未启用");
    }
}
