package com.fafeng.clinic.ai.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fafeng.clinic.ai.config.ExternalServiceConfigService;
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
public class HttpOcrClient implements OcrClient {

    private static final Logger log = LoggerFactory.getLogger(HttpOcrClient.class);

    private final ExternalServiceConfigService externalServiceConfigService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public HttpOcrClient(ExternalServiceConfigService externalServiceConfigService,
                         ObjectMapper objectMapper) {
        this.externalServiceConfigService = externalServiceConfigService;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(10));
        requestFactory.setReadTimeout(Duration.ofSeconds(180));
        this.restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Override
    public boolean isConfigured() {
        return externalServiceConfigService.isOcrEnabled()
                && resolveOcrUrl() != null
                && !resolveOcrUrl().isBlank();
    }

    @Override
    public String recognize(byte[] imageBytes, String filename, String contentType) {
        if (!isConfigured()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "OCR 服务未配置");
        }
        if (imageBytes == null || imageBytes.length == 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片文件为空");
        }

        String baseUrl = resolveOcrUrl().trim().replaceAll("/+$", "");
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new ByteArrayResource(imageBytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        });

        try {
            String responseBody = restClient.post()
                    .uri(baseUrl + "/ocr")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return parseText(responseBody);
        } catch (RestClientException ex) {
            log.warn("OCR recognition failed: {}", ex.getMessage());
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "OCR 服务暂不可用，请稍后重试或手动入库");
        }
    }

    private String resolveOcrUrl() {
        return externalServiceConfigService.getOcrUrl();
    }

    private String parseText(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "OCR 服务返回空结果");
        }
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            String text = root.path("text").asText("").trim();
            if (text.isEmpty()) {
                throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "未识别到有效文字，请确认是清晰的打印版单据");
            }
            return text;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "OCR 结果解析失败");
        }
    }
}
