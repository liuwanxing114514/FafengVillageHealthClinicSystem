package com.fafeng.clinic.ai.config;

import com.fafeng.clinic.ai.advisor.DesensitizationAdvisor;
import com.fafeng.clinic.ai.client.AiChatClient;
import com.fafeng.clinic.ai.client.SpringAiChatClient;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Spring AI 条件装配：仅当 {@code clinic.ai.enabled=true} 且 {@code provider=deepseek} 时创建 ChatClient。
 * 主通道失败（429/503 等）且配置了 {@code deepseek-fallback-api-key} 时，自动切换 DeepSeek 官方 API。
 */
@Configuration
@ConditionalOnExpression("${clinic.ai.enabled:false} == true and '${clinic.ai.provider:noop}' == 'deepseek'")
public class SpringAiConfiguration {

    @Bean
    public OpenAiChatModel clinicOpenAiChatModel(ClinicAiProperties properties) {
        return buildChatModel(
                properties.getDeepseekBaseUrl(),
                properties.getDeepseekApiKey(),
                properties.getDeepseekModel());
    }

    @Bean
    public ChatClient clinicChatClient(OpenAiChatModel chatModel, DesensitizationAdvisor desensitizationAdvisor) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(desensitizationAdvisor)
                .build();
    }

    @Bean
    @Primary
    public AiChatClient springAiChatClient(
            ClinicAiProperties properties,
            ChatClient clinicChatClient,
            DesensitizationAdvisor desensitizationAdvisor) {
        ChatClient fallbackClient = null;
        if (properties.hasDeepseekFallback()) {
            OpenAiChatModel fallbackModel = buildChatModel(
                    properties.getDeepseekFallbackBaseUrl(),
                    properties.getDeepseekFallbackApiKey(),
                    properties.getDeepseekFallbackModel());
            fallbackClient = ChatClient.builder(fallbackModel)
                    .defaultAdvisors(desensitizationAdvisor)
                    .build();
        }
        return new SpringAiChatClient(properties, clinicChatClient, fallbackClient);
    }

    private static OpenAiChatModel buildChatModel(String baseUrl, String apiKey, String model) {
        String normalizedBaseUrl = baseUrl == null ? "" : baseUrl.trim().replaceAll("/+$", "");
        String normalizedKey = apiKey == null ? "" : apiKey.trim();
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(normalizedBaseUrl)
                .apiKey(normalizedKey)
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(0.2)
                        .build())
                .build();
    }
}
