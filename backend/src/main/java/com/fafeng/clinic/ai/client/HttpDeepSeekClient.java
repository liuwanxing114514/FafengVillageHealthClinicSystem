package com.fafeng.clinic.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fafeng.clinic.ai.config.ClinicAiProperties;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.List;
import java.util.Map;

@Component
public class HttpDeepSeekClient implements DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(HttpDeepSeekClient.class);

    private final ClinicAiProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public HttpDeepSeekClient(ClinicAiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(120));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public boolean isConfigured() {
        return properties.isEnabled()
                && properties.getDeepseekApiKey() != null
                && !properties.getDeepseekApiKey().isBlank();
    }

    @Override
    public String chatCompletion(String systemPrompt, String userMessage) {
        if (!isConfigured()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "DeepSeek 未配置");
        }
        String baseUrl = properties.getDeepseekBaseUrl().trim().replaceAll("/+$", "");
        Map<String, Object> body = Map.of(
                "model", properties.getDeepseekModel(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)
                ),
                "temperature", 0.2
        );
        try {
            String responseBody = restClient.post()
                    .uri(baseUrl + "/v1/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.getDeepseekApiKey().trim())
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return parseContent(responseBody);
        } catch (RestClientException ex) {
            log.warn("DeepSeek request failed: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 服务暂不可用，请稍后重试或手动录入");
        }
    }

    private String parseContent(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 服务返回空结果");
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode content = root.path("choices").path(0).path("message").path("content");
            String text = content.asText("").trim();
            if (text.isEmpty()) {
                throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 未返回有效内容");
            }
            return text;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 结果解析失败");
        }
    }
}
