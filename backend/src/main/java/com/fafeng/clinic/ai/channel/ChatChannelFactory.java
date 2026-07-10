package com.fafeng.clinic.ai.channel;

import com.fafeng.clinic.ai.advisor.DesensitizationAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class ChatChannelFactory {

    private final DesensitizationAdvisor desensitizationAdvisor;

    public ChatChannelFactory(DesensitizationAdvisor desensitizationAdvisor) {
        this.desensitizationAdvisor = desensitizationAdvisor;
    }

    public List<ChatChannelRuntime> buildRuntimes(List<ChatChannelConfig> configs) {
        List<ChatChannelRuntime> runtimes = new ArrayList<>();
        for (ChatChannelConfig config : configs) {
            if (!config.isUsable()) {
                continue;
            }
            runtimes.add(new ChatChannelRuntime(config, buildClient(config)));
        }
        return List.copyOf(runtimes);
    }

    public ChatClient buildClient(ChatChannelConfig config) {
        String baseUrl = normalizeBaseUrl(config.baseUrl());
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(config.apiKey().trim())
                .build();
        double temperature = config.temperature() == null ? 0.2 : config.temperature().doubleValue();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(config.model())
                        .temperature(temperature)
                        .build())
                .build();
        return ChatClient.builder(chatModel)
                .defaultAdvisors(desensitizationAdvisor)
                .build();
    }

    static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            return "";
        }
        return baseUrl.trim().replaceAll("/+$", "");
    }
}
