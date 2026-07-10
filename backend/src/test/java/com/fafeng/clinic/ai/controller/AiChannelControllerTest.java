package com.fafeng.clinic.ai.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.fafeng.clinic.ai.channel.ChannelRegistry;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@TestPropertySource(properties = {
        "clinic.settings.encryption-key=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
        "clinic.ai.enabled=true",
        "clinic.ai.deepseek-api-key=test-key-for-list"
})
class AiChannelControllerTest {

    /** 避免启动时从 dev 库解密历史通道密钥导致 ApplicationContext 加载失败 */
    @MockBean
    private ChannelRegistry channelRegistry;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanAiConfigTables() {
        jdbcTemplate.update("DELETE FROM ai_chat_channel");
        jdbcTemplate.update("DELETE FROM ai_embedding_channel");
        jdbcTemplate.update("DELETE FROM external_service");
    }

    @Test
    @WithMockUser(username = "admin")
    void listExternalServices() throws Exception {
        mockMvc.perform(get("/api/ai/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.services.chat").exists())
                .andExpect(jsonPath("$.data.services.embedding").exists());
    }

    @Test
    @WithMockUser(username = "admin")
    void listChatChannelsFromEnvBootstrap() throws Exception {
        mockMvc.perform(get("/api/ai/channels/chat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    @WithMockUser(username = "admin")
    void listEmbeddingChannelsFromEnvBootstrap() throws Exception {
        mockMvc.perform(get("/api/ai/channels/embedding"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data").isArray());
    }
}
