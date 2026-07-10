package com.fafeng.clinic.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.OffsetDateTime;

@TableName("external_service")
public class ExternalService {

    public static final String CODE_CHAT = "chat";
    public static final String CODE_EMBEDDING = "embedding";
    public static final String CODE_WHISPER = "whisper";
    public static final String CODE_OCR = "ocr";

    @TableId(value = "service_code", type = IdType.INPUT)
    private String serviceCode;
    private Boolean enabled;
    private String endpointUrl;
    private String optionsJson;
    private OffsetDateTime updatedAt;

    public String getServiceCode() {
        return serviceCode;
    }

    public void setServiceCode(String serviceCode) {
        this.serviceCode = serviceCode;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public String getEndpointUrl() {
        return endpointUrl;
    }

    public void setEndpointUrl(String endpointUrl) {
        this.endpointUrl = endpointUrl;
    }

    public String getOptionsJson() {
        return optionsJson;
    }

    public void setOptionsJson(String optionsJson) {
        this.optionsJson = optionsJson;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
