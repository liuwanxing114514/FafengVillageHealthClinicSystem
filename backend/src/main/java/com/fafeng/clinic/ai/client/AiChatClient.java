package com.fafeng.clinic.ai.client;

import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
 * 诊所 AI 聊天客户端抽象。生产环境由 Spring AI {@code ChatClient} 实现；测试可 mock。
 */
public interface AiChatClient {

    boolean isConfigured();

    String chatCompletion(String systemPrompt, String userMessage);

    default String chatCompletion(String systemPrompt, String userMessage, boolean skipDesensitization) {
        return chatCompletion(systemPrompt, userMessage);
    }

    String chatWithTools(String systemPrompt, String userMessage, Object toolSource);

    String chatWithTools(String systemPrompt, List<Message> history, String userMessage, Object toolSource);
}
