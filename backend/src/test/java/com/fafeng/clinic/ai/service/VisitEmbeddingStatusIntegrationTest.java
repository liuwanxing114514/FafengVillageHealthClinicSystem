package com.fafeng.clinic.ai.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("dev")
@TestPropertySource(properties = {
        "clinic.ai.embedding.enabled=true",
        "clinic.ai.embedding.api-key=test-key",
        "clinic.ai.embedding.base-url=https://api.siliconflow.cn/v1"
})
class VisitEmbeddingStatusIntegrationTest {

    @Autowired
    private VisitEmbeddingService visitEmbeddingService;

    @Test
    void getStatusWithEmbeddingEnabled() {
        var status = visitEmbeddingService.getStatus();
        assertNotNull(status);
        assertTrue(status.enabled());
        assertEquals(1024, status.dimensions());
        assertTrue(status.configured());
    }
}
