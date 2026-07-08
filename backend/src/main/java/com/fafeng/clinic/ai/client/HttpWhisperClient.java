package com.fafeng.clinic.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fafeng.clinic.ai.config.ClinicVoiceProperties;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;

@Component
public class HttpWhisperClient implements WhisperClient {

    private static final Logger log = LoggerFactory.getLogger(HttpWhisperClient.class);

    private final ClinicVoiceProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public HttpWhisperClient(ClinicVoiceProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(Duration.ofSeconds(120));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public boolean isConfigured() {
        return properties.isConfigured();
    }

    @Override
    public String transcribe(byte[] audioBytes, String filename, String contentType) {
        if (!isConfigured()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "语音转写服务未配置");
        }
        if (audioBytes == null || audioBytes.length == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "音频文件为空");
        }

        String baseUrl = properties.getWhisperUrl().trim().replaceAll("/+$", "");
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(audioBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        });
        body.add("model", "whisper-1");
        body.add("language", "zh");

        try {
            String responseBody = restClient.post()
                    .uri(baseUrl + "/v1/audio/transcriptions")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return parseText(responseBody);
        } catch (RestClientException ex) {
            log.warn("Whisper transcription failed: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "语音转写服务暂不可用，请稍后重试或手动输入");
        }
    }

    private String parseText(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "语音转写服务返回空结果");
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String text = root.path("text").asText("").trim();
            if (text.isEmpty()) {
                throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "未识别到有效语音内容");
            }
            return text;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "语音转写结果解析失败");
        }
    }
}
