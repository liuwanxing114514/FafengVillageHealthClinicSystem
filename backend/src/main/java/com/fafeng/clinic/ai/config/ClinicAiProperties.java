package com.fafeng.clinic.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clinic.ai")
public class ClinicAiProperties {

    private boolean enabled = false;
    private String provider = "noop";
    private String deepseekApiKey = "";
    private String deepseekBaseUrl = "https://api.deepseek.com";
    private String deepseekModel = "deepseek-chat";
    private String localBaseUrl = "http://localhost:11434";
    private String visitStructurePrompt = "";

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

    public String getDeepseekApiKey() {
        return deepseekApiKey;
    }

    public void setDeepseekApiKey(String deepseekApiKey) {
        this.deepseekApiKey = deepseekApiKey;
    }

    public String getDeepseekBaseUrl() {
        return deepseekBaseUrl;
    }

    public void setDeepseekBaseUrl(String deepseekBaseUrl) {
        this.deepseekBaseUrl = deepseekBaseUrl;
    }

    public String getDeepseekModel() {
        return deepseekModel;
    }

    public void setDeepseekModel(String deepseekModel) {
        this.deepseekModel = deepseekModel;
    }

    public String getLocalBaseUrl() {
        return localBaseUrl;
    }

    public void setLocalBaseUrl(String localBaseUrl) {
        this.localBaseUrl = localBaseUrl;
    }

    public String getVisitStructurePrompt() {
        return visitStructurePrompt;
    }

    public void setVisitStructurePrompt(String visitStructurePrompt) {
        this.visitStructurePrompt = visitStructurePrompt;
    }
}
