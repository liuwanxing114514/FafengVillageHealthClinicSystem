package com.fafeng.clinic.clinic.service;

import com.fafeng.clinic.system.service.SettingsService;
import com.fafeng.clinic.system.vo.SettingVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PrescriptionPrintTemplateService {

    public static final String KEY_ACTIVE = "prescription_print_active_template";
    public static final String KEY_CONFIG = "prescription_print_template_config";
    public static final String DEFAULT_ACTIVE = "default-a4";

    private final SettingsService settingsService;

    public PrescriptionPrintTemplateService(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    public String getActiveTemplate() {
        return readValue(KEY_ACTIVE, DEFAULT_ACTIVE);
    }

    public String getTemplateConfigJson() {
        return readValue(KEY_CONFIG, "{}");
    }

    public void updateActiveTemplate(String templateId) {
        settingsService.update(KEY_ACTIVE, new com.fafeng.clinic.system.dto.UpdateSettingRequest(templateId));
    }

    private String readValue(String key, String defaultValue) {
        List<SettingVO> settings = settingsService.listAll();
        return settings.stream()
                .filter(s -> key.equals(s.key()))
                .map(SettingVO::value)
                .findFirst()
                .orElse(defaultValue);
    }
}
