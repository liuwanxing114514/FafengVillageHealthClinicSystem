package com.fafeng.clinic.agent.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class AgentPrivacyCollectorTest {

    private final AgentPrivacyCollector collector = new AgentPrivacyCollector();

    @BeforeEach
    void setUp() {
        collector.activate();
    }

    @AfterEach
    void tearDown() {
        collector.clear();
    }

    @Test
    void desensitizeTextReplacesKnownPatientName() {
        collector.recordPatient("李印雪", null, null, null);
        assertEquals("最近更新的是李**", collector.desensitizeText("最近更新的是李印雪"));
    }

    @Test
    void buildContextIncludesRecordedName() {
        collector.recordPatient("张三", "13800138000", null, null);
        assertFalse(collector.desensitizeText("联系张三").contains("张三"));
    }
}
