package com.fafeng.clinic.ai.controller;

import com.fafeng.clinic.ai.service.VoiceTranscriptionService;
import com.fafeng.clinic.ai.vo.VoiceStatusVO;
import com.fafeng.clinic.ai.vo.VoiceTranscriptionVO;
import com.fafeng.clinic.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/ai/voice")
public class VoiceController {

    private final VoiceTranscriptionService voiceTranscriptionService;

    public VoiceController(VoiceTranscriptionService voiceTranscriptionService) {
        this.voiceTranscriptionService = voiceTranscriptionService;
    }

    @GetMapping("/status")
    public Result<VoiceStatusVO> status() {
        return Result.ok(voiceTranscriptionService.getStatus());
    }

    @PostMapping("/transcribe")
    public Result<VoiceTranscriptionVO> transcribe(@RequestParam("file") MultipartFile file) {
        return Result.ok(voiceTranscriptionService.transcribe(file));
    }
}
