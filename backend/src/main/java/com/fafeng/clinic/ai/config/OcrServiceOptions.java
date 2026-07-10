package com.fafeng.clinic.ai.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * OCR 外部服务 {@code options_json} 解析与构建。
 */
public final class OcrServiceOptions {

    public static final String MODE_LOCAL = "local";
    public static final String MODE_VISION = "vision";
    public static final String DEFAULT_VISION_MODEL = "Pro/Qwen/Qwen2.5-VL-7B-Instruct";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private OcrServiceOptions() {
    }

    public static String defaultVisionOptionsJson() {
        return toJson(MODE_VISION, DEFAULT_VISION_MODEL);
    }

    public static String defaultLocalOptionsJson() {
        return toJson(MODE_LOCAL, DEFAULT_VISION_MODEL);
    }

    public static String toJson(String mode, String visionModel) {
        ObjectNode node = MAPPER.createObjectNode();
        node.put("mode", normalizeMode(mode));
        if (MODE_VISION.equals(normalizeMode(mode))) {
            node.put("visionModel", normalizeVisionModel(visionModel));
        }
        try {
            return MAPPER.writeValueAsString(node);
        } catch (JsonProcessingException ex) {
            return "{\"mode\":\"vision\",\"visionModel\":\"" + DEFAULT_VISION_MODEL + "\"}";
        }
    }

    public static String parseMode(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return MODE_VISION;
        }
        try {
            JsonNode root = MAPPER.readTree(optionsJson);
            return normalizeMode(root.path("mode").asText(MODE_VISION));
        } catch (Exception ex) {
            return MODE_VISION;
        }
    }

    public static String parseVisionModel(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return DEFAULT_VISION_MODEL;
        }
        try {
            JsonNode root = MAPPER.readTree(optionsJson);
            return normalizeVisionModel(root.path("visionModel").asText(DEFAULT_VISION_MODEL));
        } catch (Exception ex) {
            return DEFAULT_VISION_MODEL;
        }
    }

    public static String merge(String existingJson, String mode, String visionModel) {
        return toJson(mode, visionModel != null ? visionModel : parseVisionModel(existingJson));
    }

    private static String normalizeMode(String mode) {
        return MODE_LOCAL.equalsIgnoreCase(mode) ? MODE_LOCAL : MODE_VISION;
    }

    private static String normalizeVisionModel(String visionModel) {
        if (visionModel == null || visionModel.isBlank()) {
            return DEFAULT_VISION_MODEL;
        }
        return visionModel.trim();
    }
}
