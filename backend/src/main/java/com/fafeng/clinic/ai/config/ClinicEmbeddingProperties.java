package com.fafeng.clinic.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clinic.ai.embedding")
public class ClinicEmbeddingProperties {

    private boolean enabled = false;
    private String provider = "openai";
    private String apiKey = "";
    private String baseUrl = "https://api.siliconflow.cn/v1";
    private String localBaseUrl = "http://localhost:1234/v1";
    private String model = "BAAI/bge-m3";
    private int dimensions = 1024;
    private String syncCron = "";
    private int batchSize = 16;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getLocalBaseUrl() {
        return localBaseUrl;
    }

    public void setLocalBaseUrl(String localBaseUrl) {
        this.localBaseUrl = localBaseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public int getDimensions() {
        return dimensions;
    }

    public void setDimensions(int dimensions) {
        this.dimensions = dimensions;
    }

    public String getSyncCron() {
        return syncCron;
    }

    public void setSyncCron(String syncCron) {
        this.syncCron = syncCron;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public boolean isLocalProvider() {
        return provider != null && "local".equalsIgnoreCase(provider.trim());
    }

    public String resolveBaseUrl() {
        if (isLocalProvider()) {
            return localBaseUrl == null ? "" : localBaseUrl.trim();
        }
        return baseUrl == null ? "" : baseUrl.trim();
    }

    public String resolveApiKey() {
        if (apiKey == null || apiKey.isBlank()) {
            return isLocalProvider() ? "lm-studio" : "";
        }
        return apiKey.trim();
    }

    public boolean isConfigured() {
        String url = resolveBaseUrl();
        return enabled && !url.isBlank() && model != null && !model.isBlank();
    }
}
