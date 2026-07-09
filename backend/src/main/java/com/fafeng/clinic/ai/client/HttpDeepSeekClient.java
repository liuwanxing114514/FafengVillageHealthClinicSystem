package com.fafeng.clinic.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

@Component
public class HttpDeepSeekClient implements DeepSeekClient {

    private static final Logger log = LoggerFactory.getLogger(HttpDeepSeekClient.class);

    private final ClinicAiProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public HttpDeepSeekClient(ClinicAiProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(90));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public boolean isConfigured() {
        return properties.getDeepseekApiKey() != null && !properties.getDeepseekApiKey().isBlank();
    }

    @Override
    public String chatCompletion(String systemPrompt, String userMessage) {
        if (!isConfigured()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "DeepSeek API 未配置");
        }
        String baseUrl = properties.getDeepseekBaseUrl().trim().replaceAll("/+$", "");
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", properties.getDeepseekModel());
            ArrayNode messages = body.putArray("messages");
            messages.addObject().put("role", "system").put("content", systemPrompt);
            messages.addObject().put("role", "user").put("content", userMessage);
            ObjectNode responseFormat = body.putObject("response_format");
            responseFormat.put("type", "json_object");

            String responseBody = restClient.post()
                    .uri(baseUrl + "/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + properties.getDeepseekApiKey().trim())
                    .body(objectMapper.writeValueAsString(body))
                    .retrieve()
                    .body(String.class);
            return parseContent(responseBody);
        } catch (BusinessException ex) {
            throw ex;
        } catch (RestClientException ex) {
            log.warn("DeepSeek API call failed: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "DeepSeek 服务暂不可用，请稍后重试");
        } catch (Exception ex) {
            log.warn("DeepSeek API call error: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "DeepSeek 响应解析失败");
        }
    }

    private String parseContent(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "DeepSeek 返回空结果");
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode error = root.path("error");
            if (!error.isMissingNode()) {
                throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
                        "DeepSeek 调用失败：" + error.path("message").asText("未知错误"));
            }
            String content = root.path("choices").path(0).path("message").path("content").asText("").trim();
            if (content.isEmpty()) {
                throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "DeepSeek 未返回有效内容");
            }
            return content;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "DeepSeek 响应解析失败");
        }
    }
}
