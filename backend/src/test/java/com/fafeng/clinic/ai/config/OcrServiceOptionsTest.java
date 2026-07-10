package com.fafeng.clinic.ai.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OcrServiceOptionsTest {

    @Test
    void toJsonAndParseVisionMode() {
        String json = OcrServiceOptions.toJson(OcrServiceOptions.MODE_VISION, "Qwen/Qwen2.5-VL-7B-Instruct");
        assertEquals(OcrServiceOptions.MODE_VISION, OcrServiceOptions.parseMode(json));
        assertEquals("Qwen/Qwen2.5-VL-7B-Instruct", OcrServiceOptions.parseVisionModel(json));
        assertTrue(json.contains("visionModel"));
    }

    @Test
    void parseDefaultsWhenBlank() {
        assertEquals(OcrServiceOptions.MODE_VISION, OcrServiceOptions.parseMode(null));
        assertEquals(OcrServiceOptions.DEFAULT_VISION_MODEL, OcrServiceOptions.parseVisionModel(""));
    }
}
