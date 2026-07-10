package com.fafeng.clinic.ai.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fafeng.clinic.ai.config.ExternalServiceConfigService;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "clinic.ai.enabled=false",
        "clinic.ai.provider=noop"
})
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ExternalServiceConfigService externalServiceConfigService;

    @BeforeEach
    void resetAiServiceFlags() {
        jdbcTemplate.update("DELETE FROM external_service");
        jdbcTemplate.update("DELETE FROM ai_chat_channel");
        jdbcTemplate.update("DELETE FROM ai_embedding_channel");
        externalServiceConfigService.refresh();
    }

    @Test
    @WithMockUser(username = "admin")
    void statusAndDraftCrud() throws Exception {
        mockMvc.perform(get("/api/ai/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.enabled").value(false))
                .andExpect(jsonPath("$.data.provider").value("noop"));

        String createResponse = mockMvc.perform(post("/api/ai/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "draftType": "QUERY",
                                  "payload": "{\\"question\\":\\"库存不足有哪些\\"}"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.draftType").value("QUERY"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = com.fasterxml.jackson.databind.json.JsonMapper.builder().build()
                .readTree(createResponse)
                .path("data")
                .path("id")
                .asLong();

        mockMvc.perform(get("/api/ai/drafts/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(id));

        mockMvc.perform(get("/api/ai/drafts?draftType=QUERY&status=PENDING"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data", hasSize(1)));

        mockMvc.perform(patch("/api/ai/drafts/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("APPROVED"));
    }
}
