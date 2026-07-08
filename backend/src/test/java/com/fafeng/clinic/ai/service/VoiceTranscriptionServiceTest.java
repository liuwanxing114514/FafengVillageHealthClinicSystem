package com.fafeng.clinic.ai.service;

import com.fafeng.clinic.ai.client.WhisperClient;
import com.fafeng.clinic.ai.config.ClinicVoiceProperties;
import com.fafeng.clinic.common.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class VoiceTranscriptionServiceTest {

    private ClinicVoiceProperties properties;
    private WhisperClient whisperClient;
    private VoiceTranscriptionService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        properties = new ClinicVoiceProperties();
        properties.setWhisperUrl("http://whisper-service:9000");
        properties.setUploadDir(tempDir.toString());
        properties.setSaveAudio(true);
        whisperClient = mock(WhisperClient.class);
        service = new VoiceTranscriptionService(properties, whisperClient);
    }

    @Test
    void transcribeSavesAudioAndReturnsText() {
        when(whisperClient.transcribe(any(), anyString(), anyString())).thenReturn(" 主诉内容 ");

        var result = service.transcribe(new MockMultipartFile(
                "file",
                "recording.webm",
                "audio/webm",
                new byte[] {9, 8, 7}));

        assertEquals("主诉内容", result.text());
    }

    @Test
    void transcribeRejectsOversizedFile() {
        byte[] large = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "recording.webm", "audio/webm", large);

        assertThrows(BusinessException.class, () -> service.transcribe(file));
    }
}
