package com.fafeng.clinic.ai.provider;

public interface AiProvider {

    String name();

    boolean isAvailable();

    /**
     * 调用大模型聊天补全，返回 assistant 文本。
     */
    String chatCompletion(String systemPrompt, String userMessage);

    default String chatCompletion(String systemPrompt, String userMessage, boolean skipDesensitization) {
        return chatCompletion(systemPrompt, userMessage);
    }
}
