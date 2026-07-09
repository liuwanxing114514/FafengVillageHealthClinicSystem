package com.fafeng.clinic.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clinic.ocr")
public class ClinicOcrProperties {

    private String ocrUrl = "";
    private String uploadDir = "uploads/images";

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
}
