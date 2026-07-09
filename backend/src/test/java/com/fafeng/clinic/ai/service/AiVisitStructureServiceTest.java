package com.fafeng.clinic.ai.service;

import com.fafeng.clinic.ai.client.AiChatClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "clinic.ai.enabled=true",
        "clinic.ai.provider=deepseek",
        "clinic.ai.deepseek-api-key=test-key"
})
class AiVisitStructureServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiChatClient aiChatClient;

    @Test
    @WithMockUser(username = "admin")
    void structureVisitCreatesDraft() throws Exception {
        when(aiChatClient.isConfigured()).thenReturn(true);
        when(aiChatClient.chatCompletion(anyString(), anyString())).thenReturn("""
                {
                  "chiefComplaint": "咳嗽3天",
                  "presentIllness": "受凉后咳嗽",
                  "pastHistory": "",
                  "allergyHistory": "",
                  "diagnosis": "上呼吸道感染",
                  "treatment": "对症处理",
                  "remark": ""
                }
                """);

        mockMvc.perform(post("/api/ai/structure/visit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"text":"患者说咳嗽三天，诊断上呼吸道感染"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.draftType").value("VISIT"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }
}
