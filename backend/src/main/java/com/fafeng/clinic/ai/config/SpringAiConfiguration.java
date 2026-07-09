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
 */
@Configuration
@ConditionalOnExpression("${clinic.ai.enabled:false} == true and '${clinic.ai.provider:noop}' == 'deepseek'")
public class SpringAiConfiguration {

    @Bean
    public OpenAiChatModel clinicOpenAiChatModel(ClinicAiProperties properties) {
        String baseUrl = properties.getDeepseekBaseUrl().trim().replaceAll("/+$", "");
        String apiKey = properties.getDeepseekApiKey() == null ? "" : properties.getDeepseekApiKey().trim();
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(baseUrl)
                .apiKey(apiKey)
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(properties.getDeepseekModel())
                        .temperature(0.2)
                        .build())
                .build();
    }

    @Bean
    public ChatClient clinicChatClient(OpenAiChatModel chatModel, DesensitizationAdvisor desensitizationAdvisor) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(desensitizationAdvisor)
                .build();
    }

    @Bean
    @Primary
    public AiChatClient springAiChatClient(ClinicAiProperties properties, ChatClient clinicChatClient) {
        return new SpringAiChatClient(properties, clinicChatClient);
    }
}
