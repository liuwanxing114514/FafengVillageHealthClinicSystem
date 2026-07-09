package com.fafeng.clinic.ai.client;

public interface DeepSeekClient {

    boolean isConfigured();

    String chatCompletion(String systemPrompt, String userMessage);
}
