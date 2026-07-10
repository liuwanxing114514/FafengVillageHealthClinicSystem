package com.fafeng.clinic.ai.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "clinic.ai.enabled=false",
        "clinic.ai.embedding.enabled=false",
        "clinic.voice.whisper-url=",
        "clinic.ocr.ocr-url="
})
class ExternalServiceConfigServiceTest {

    @Autowired
    private ExternalServiceConfigService externalServiceConfigService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDbConfig() {
        jdbcTemplate.update("DELETE FROM ai_chat_channel");
        jdbcTemplate.update("DELETE FROM ai_embedding_channel");
        jdbcTemplate.update("DELETE FROM external_service");
        externalServiceConfigService.refresh();
    }

    @Test
    void bootstrapFromEnvWhenDbEmpty() {
        assertFalse(externalServiceConfigService.isDbBacked());
        assertFalse(externalServiceConfigService.isChatEnabled());
        assertFalse(externalServiceConfigService.isEmbeddingEnabled());
        assertFalse(externalServiceConfigService.isWhisperEnabled());
        assertFalse(externalServiceConfigService.isOcrEnabled());
    }
}

@SpringBootTest
@Transactional
@TestPropertySource(properties = {
        "clinic.ai.enabled=true",
        "clinic.ai.deepseek-api-key=test-key",
        "clinic.ai.embedding.enabled=true",
        "clinic.ai.embedding.api-key=embed-key"
})
class ExternalServiceConfigServiceBootstrapEnabledTest {

    @Autowired
    private ExternalServiceConfigService externalServiceConfigService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDbConfig() {
        jdbcTemplate.update("DELETE FROM ai_chat_channel");
        jdbcTemplate.update("DELETE FROM ai_embedding_channel");
        jdbcTemplate.update("DELETE FROM external_service");
        externalServiceConfigService.refresh();
    }

    @Test
    void bootstrapReflectsEnabledEnvFlags() {
        assertFalse(externalServiceConfigService.isDbBacked());
        assertTrue(externalServiceConfigService.isChatEnabled());
        assertTrue(externalServiceConfigService.isEmbeddingEnabled());
    }
}
