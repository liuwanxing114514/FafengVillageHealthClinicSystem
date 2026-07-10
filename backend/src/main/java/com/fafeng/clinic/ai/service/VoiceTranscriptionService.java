package com.fafeng.clinic.ai.service;

import com.fafeng.clinic.ai.client.WhisperClient;
import com.fafeng.clinic.ai.config.ClinicVoiceProperties;
import com.fafeng.clinic.ai.vo.VoiceStatusVO;
import com.fafeng.clinic.ai.vo.VoiceTranscriptionVO;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class VoiceTranscriptionService {

    private static final Logger log = LoggerFactory.getLogger(VoiceTranscriptionService.class);
    private static final long MAX_AUDIO_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "audio/webm",
            "audio/wav",
            "audio/x-wav",
            "audio/mpeg",
            "audio/mp4",
            "audio/ogg",
            "application/octet-stream"
    );

    private final ClinicVoiceProperties properties;
    private final WhisperClient whisperClient;

    public VoiceTranscriptionService(ClinicVoiceProperties properties, WhisperClient whisperClient) {
        this.properties = properties;
        this.whisperClient = whisperClient;
    }

    public VoiceStatusVO getStatus() {
        boolean configured = whisperClient.isConfigured();
        return new VoiceStatusVO(configured, configured);
    }

    public VoiceTranscriptionVO transcribe(MultipartFile file) {
        validateFile(file);
        byte[] audioBytes = readBytes(file);
        saveAudioIfEnabled(audioBytes, file.getOriginalFilename(), file.getContentType());
        String text = whisperClient.transcribe(
                audioBytes,
                resolveFilename(file),
                file.getContentType()
        );
        return new VoiceTranscriptionVO(text.trim());
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "请上传音频文件");
        }
        if (file.getSize() > MAX_AUDIO_BYTES) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "音频文件不能超过 10MB");
        }
        String contentType = normalizeContentType(file.getContentType());
        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的音频格式");
        }
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException ex) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "读取音频文件失败");
        }
    }

    private void saveAudioIfEnabled(byte[] audioBytes, String originalFilename, String contentType) {
        if (!properties.isSaveAudio()) {
            return;
        }
        try {
            String extension = guessExtension(originalFilename, contentType);
            Path dir = Path.of(properties.getUploadDir(), LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE));
            Files.createDirectories(dir);
            Path target = dir.resolve(UUID.randomUUID() + extension);
            Files.write(target, audioBytes);
        } catch (IOException ex) {
            log.warn("Failed to save voice audio: {}", ex.getMessage());
        }
    }

    private String resolveFilename(MultipartFile file) {
        String original = file.getOriginalFilename();
        if (original != null && !original.isBlank()) {
            return original.trim();
        }
        return "recording" + guessExtension(null, file.getContentType());
    }

    private String guessExtension(String originalFilename, String contentType) {
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        String normalized = normalizeContentType(contentType);
        return switch (normalized) {
            case "audio/wav", "audio/x-wav" -> ".wav";
            case "audio/mpeg" -> ".mp3";
            case "audio/mp4" -> ".m4a";
            case "audio/ogg" -> ".ogg";
            default -> ".webm";
        };
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "application/octet-stream";
        }
        return contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
    }
}
