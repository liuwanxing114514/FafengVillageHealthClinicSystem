package com.fafeng.clinic.ai.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class QuickPhraseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @WithMockUser(username = "admin")
    void listFields() throws Exception {
        mockMvc.perform(get("/api/quick-phrases/fields"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].key").exists());
    }

    @Test
    @WithMockUser(username = "admin")
    void createListUseAndDelete() throws Exception {
        String createJson = mockMvc.perform(post("/api/quick-phrases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fieldKey":"diagnosis","content":"单元测试上呼吸道感染"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.content").value("单元测试上呼吸道感染"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        long id = objectMapper.readTree(createJson).path("data").path("id").asLong();

        mockMvc.perform(get("/api/quick-phrases/candidates")
                        .param("fieldKey", "diagnosis"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[?(@.content == '单元测试上呼吸道感染')].content",
                        org.hamcrest.Matchers.hasItem("单元测试上呼吸道感染")));

        mockMvc.perform(get("/api/quick-phrases")
                        .param("fieldKey", "diagnosis"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/quick-phrases/" + id + "/use"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.useCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        mockMvc.perform(put("/api/quick-phrases/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fieldKey":"diagnosis","content":"急性上呼吸道感染"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("急性上呼吸道感染"));

        mockMvc.perform(delete("/api/quick-phrases/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @WithMockUser(username = "admin")
    void rejectsInvalidField() throws Exception {
        mockMvc.perform(get("/api/quick-phrases/candidates")
                        .param("fieldKey", "invalid_field"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}
