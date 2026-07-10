package com.fafeng.clinic.ai.channel;

import org.springframework.ai.chat.client.ChatClient;

public record ChatChannelRuntime(ChatChannelConfig config, ChatClient chatClient) {
}
