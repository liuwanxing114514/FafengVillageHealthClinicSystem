package com.fafeng.clinic.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clinic.ocr")
public class ClinicOcrProperties {

    private String ocrUrl = "";
    private String uploadDir = "uploads/images";
    private String mode = "";
    private String visionModel = OcrServiceOptions.DEFAULT_VISION_MODEL;

    public boolean isConfigured() {
        return ocrUrl != null && !ocrUrl.isBlank();
    }

    public String getOcrUrl() {
        return ocrUrl;
    }

    public void setOcrUrl(String ocrUrl) {
        this.ocrUrl = ocrUrl;
    }

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public String getVisionModel() {
        return visionModel;
    }

    public void setVisionModel(String visionModel) {
        this.visionModel = visionModel;
    }

    public String resolveMode() {
        if (mode != null && !mode.isBlank()) {
            return OcrServiceOptions.MODE_LOCAL.equalsIgnoreCase(mode)
                    ? OcrServiceOptions.MODE_LOCAL
                    : OcrServiceOptions.MODE_VISION;
        }
        return isConfigured() ? OcrServiceOptions.MODE_LOCAL : OcrServiceOptions.MODE_VISION;
    }
}
