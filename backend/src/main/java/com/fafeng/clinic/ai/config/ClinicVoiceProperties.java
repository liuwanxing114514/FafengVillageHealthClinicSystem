package com.fafeng.clinic.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "clinic.voice")
public class ClinicVoiceProperties {

    private String whisperUrl = "";
    private String uploadDir = "uploads/audio";
    private boolean saveAudio = true;

    public String getWhisperUrl() {
        return whisperUrl;
    }

    public void setWhisperUrl(String whisperUrl) {
        this.whisperUrl = whisperUrl;
    }

    public String getUploadDir() {
        return uploadDir;
    }

    public void setUploadDir(String uploadDir) {
        this.uploadDir = uploadDir;
    }

    public boolean isSaveAudio() {
        return saveAudio;
    }

    public void setSaveAudio(boolean saveAudio) {
        this.saveAudio = saveAudio;
    }

    public boolean isConfigured() {
        return whisperUrl != null && !whisperUrl.isBlank();
    }
}
