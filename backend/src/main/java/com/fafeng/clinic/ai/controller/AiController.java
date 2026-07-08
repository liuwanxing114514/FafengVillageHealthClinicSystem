package com.fafeng.clinic.ai.controller;

import com.fafeng.clinic.ai.dto.CreateAiDraftRequest;
import com.fafeng.clinic.ai.dto.UpdateAiDraftStatusRequest;
import com.fafeng.clinic.ai.service.AiDraftService;
import com.fafeng.clinic.ai.vo.AiDraftVO;
import com.fafeng.clinic.ai.vo.AiStatusVO;
import com.fafeng.clinic.common.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiDraftService aiDraftService;

    public AiController(AiDraftService aiDraftService) {
        this.aiDraftService = aiDraftService;
    }

    @GetMapping("/status")
    public Result<AiStatusVO> status() {
        return Result.ok(aiDraftService.getStatus());
    }

    @GetMapping("/drafts")
    public Result<List<AiDraftVO>> listDrafts(
            @RequestParam(required = false) String draftType,
            @RequestParam(required = false) String status) {
        return Result.ok(aiDraftService.list(draftType, status));
    }

    @PostMapping("/drafts")
    public Result<AiDraftVO> createDraft(@Valid @RequestBody CreateAiDraftRequest request) {
        return Result.ok(aiDraftService.create(request));
    }

    @GetMapping("/drafts/{id}")
    public Result<AiDraftVO> getDraft(@PathVariable Long id) {
        return Result.ok(aiDraftService.get(id));
    }

    @PatchMapping("/drafts/{id}")
    public Result<AiDraftVO> updateDraftStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAiDraftStatusRequest request) {
        return Result.ok(aiDraftService.updateStatus(id, request));
    }
}
