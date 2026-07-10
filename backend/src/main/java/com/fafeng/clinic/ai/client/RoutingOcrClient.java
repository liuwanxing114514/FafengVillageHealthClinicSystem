package com.fafeng.clinic.ai.client;

import com.fafeng.clinic.ai.config.ExternalServiceConfigService;
import com.fafeng.clinic.ai.config.OcrServiceOptions;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * 按设置页 OCR 模式路由：{@code local} → PaddleOCR；{@code vision} → 多模态 API。
 */
@Component
@Primary
public class RoutingOcrClient implements OcrClient {

    private final ExternalServiceConfigService externalServiceConfigService;
    private final HttpOcrClient localOcrClient;
    private final VisionOcrClient visionOcrClient;

    public RoutingOcrClient(ExternalServiceConfigService externalServiceConfigService,
                            HttpOcrClient localOcrClient,
                            VisionOcrClient visionOcrClient) {
        this.externalServiceConfigService = externalServiceConfigService;
        this.localOcrClient = localOcrClient;
        this.visionOcrClient = visionOcrClient;
    }

    @Override
    public boolean isConfigured() {
        if (!externalServiceConfigService.isOcrEnabled()) {
            return false;
        }
        if (OcrServiceOptions.MODE_LOCAL.equals(externalServiceConfigService.getOcrMode())) {
            return localOcrClient.isConfigured();
        }
        return visionOcrClient.isConfigured();
    }

    @Override
    public String recognize(byte[] imageBytes, String filename, String contentType) {
        if (OcrServiceOptions.MODE_LOCAL.equals(externalServiceConfigService.getOcrMode())) {
            return localOcrClient.recognize(imageBytes, filename, contentType);
        }
        return visionOcrClient.recognize(imageBytes, filename, contentType);
    }
}
