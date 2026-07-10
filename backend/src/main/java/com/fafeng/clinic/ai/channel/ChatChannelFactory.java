package com.fafeng.clinic.ai.channel;

import com.fafeng.clinic.ai.advisor.DesensitizationAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
public class ChatChannelFactory {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(90);

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
                .restClientBuilder(RestClient.builder().requestFactory(httpRequestFactory()))
                .build();
        double temperature = config.temperature() == null ? 0.2 : config.temperature().doubleValue();
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .retryTemplate(singleAttemptRetryTemplate())
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(config.model())
                        .temperature(temperature)
                        .build())
                .build();
        return ChatClient.builder(chatModel)
                .defaultAdvisors(desensitizationAdvisor)
                .build();
    }

    /** 503/限流等由多通道 failover 处理，单通道内不再 Spring 重试 10 次拖住 HTTP 响应 */
    private static RetryTemplate singleAttemptRetryTemplate() {
        RetryTemplate template = new RetryTemplate();
        SimpleRetryPolicy policy = new SimpleRetryPolicy();
        policy.setMaxAttempts(1);
        template.setRetryPolicy(policy);
        return template;
    }

    private static SimpleClientHttpRequestFactory httpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(CONNECT_TIMEOUT);
        factory.setReadTimeout(READ_TIMEOUT);
        return factory;
    }

    static String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null) {
            return "";
        }
        return baseUrl.trim().replaceAll("/+$", "");
    }
}
