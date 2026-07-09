package com.fafeng.clinic.agent.controller;

import com.fafeng.clinic.agent.tool.ClinicAgentTools;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class AgentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiChatClient aiChatClient;

    @Test
    @WithMockUser(username = "admin")
    void chatWithToolCallAndFinalAnswer() throws Exception {
        when(aiChatClient.isConfigured()).thenReturn(true);
        when(aiChatClient.chatWithTools(anyString(), anyString(), any()))
                .thenAnswer(invocation -> {
                    Object tools = invocation.getArgument(2);
                    if (tools instanceof ClinicAgentTools agentTools) {
                        agentTools.queryExpiringMedicine();
                    }
                    return "当前有 2 种药品即将过期，请关注临期预警页。";
                });

        mockMvc.perform(post("/api/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"message":"哪些药快过期了？"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.answer").value("当前有 2 种药品即将过期，请关注临期预警页。"))
                .andExpect(jsonPath("$.data.toolCalls[0].toolName").value("queryExpiringMedicine"))
                .andExpect(jsonPath("$.data.sessionId").isNotEmpty());
    }

    @Test
    @WithMockUser(username = "admin")
    void listRecentLogs() throws Exception {
        when(aiChatClient.isConfigured()).thenReturn(true);
        when(aiChatClient.chatWithTools(anyString(), anyString(), any()))
                .thenReturn("暂无临期药品");

        mockMvc.perform(post("/api/agent/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"临期药品\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/agent/logs?limit=5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }
}
