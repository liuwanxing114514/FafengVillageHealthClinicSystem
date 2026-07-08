package com.fafeng.clinic.ai.controller;

import com.fafeng.clinic.ai.client.WhisperClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class VoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WhisperClient whisperClient;

    @Test
    @WithMockUser(username = "admin")
    void statusWhenNotConfigured() throws Exception {
        mockMvc.perform(get("/api/ai/voice/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.configured").value(false))
                .andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    @WithMockUser(username = "admin")
    void transcribeReturnsText() throws Exception {
        when(whisperClient.isConfigured()).thenReturn(true);
        when(whisperClient.transcribe(any(), anyString(), anyString())).thenReturn("咳嗽三天");

        mockMvc.perform(multipart("/api/ai/voice/transcribe")
                        .file(new MockMultipartFile(
                                "file",
                                "recording.webm",
                                "audio/webm",
                                new byte[] {1, 2, 3, 4})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.text").value("咳嗽三天"));
    }

    @Test
    @WithMockUser(username = "admin")
    void transcribeRejectsEmptyFile() throws Exception {
        mockMvc.perform(multipart("/api/ai/voice/transcribe")
                        .file(new MockMultipartFile(
                                "file",
                                "recording.webm",
                                "audio/webm",
                                new byte[0])))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }
}
