package com.fafeng.clinic.ai.client;

import com.fafeng.clinic.ai.channel.ChannelInvocationHelper;
import com.fafeng.clinic.ai.channel.ChannelRegistry;
import com.fafeng.clinic.ai.channel.ChatChannelRuntime;
import com.fafeng.clinic.ai.config.ExternalServiceConfigService;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
@Primary
public class ResilientAiChatClient implements AiChatClient {

    private final ExternalServiceConfigService externalServiceConfigService;
    private final ChannelRegistry channelRegistry;

    public ResilientAiChatClient(ExternalServiceConfigService externalServiceConfigService,
                                 ChannelRegistry channelRegistry) {
        this.externalServiceConfigService = externalServiceConfigService;
        this.channelRegistry = channelRegistry;
    }

    @Override
    public boolean isConfigured() {
        return externalServiceConfigService.isChatEnabled() && channelRegistry.hasUsableChatChannels();
    }

    @Override
    public String chatCompletion(String systemPrompt, String userMessage) {
        requireConfigured();
        return invokeWithFailover(
                channel -> channel.chatClient().prompt()
                        .system(systemPrompt)
                        .user(userMessage)
                        .call()
                        .content());
    }

    @Override
    public String chatWithTools(String systemPrompt, String userMessage, Object toolSource) {
        requireConfigured();
        return invokeWithFailover(
                channel -> channel.chatClient().prompt()
                        .system(systemPrompt)
                        .user(userMessage)
                        .tools(toolSource)
                        .call()
                        .content());
    }

    private String invokeWithFailover(java.util.function.Function<ChatChannelRuntime, String> call) {
        List<ChatChannelRuntime> channels = channelRegistry.chatChannels();
        List<String> labels = channels.stream()
                .map(c -> c.config().label())
                .collect(Collectors.toList());
        return ChannelInvocationHelper.invokeWithFailover(labels, index -> {
            String content = call.apply(channels.get(index));
            return requireContent(content);
        });
    }

    private void requireConfigured() {
        if (!isConfigured()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "DeepSeek 未配置");
        }
    }

    private String requireContent(String content) {
        if (content == null || content.isBlank()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 未返回有效内容");
        }
        return content.trim();
    }
}
