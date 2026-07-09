package com.fafeng.clinic.ai.controller;

import com.fafeng.clinic.ai.dto.ApproveVisitDraftRequest;
import com.fafeng.clinic.ai.dto.CreateAiDraftRequest;
import com.fafeng.clinic.ai.dto.StructureVisitRequest;
import com.fafeng.clinic.ai.dto.UpdateAiDraftPayloadRequest;
import com.fafeng.clinic.ai.dto.UpdateAiDraftStatusRequest;
import com.fafeng.clinic.ai.service.AiDraftService;
import com.fafeng.clinic.ai.service.AiInboundOcrService;
import com.fafeng.clinic.ai.service.AiVisitStructureService;
import com.fafeng.clinic.ai.vo.AiDraftVO;
import com.fafeng.clinic.ai.vo.AiStatusVO;
import com.fafeng.clinic.ai.vo.ApproveInboundResultVO;
import com.fafeng.clinic.ai.vo.OcrStatusVO;
import com.fafeng.clinic.clinic.vo.VisitDetailVO;
import com.fafeng.clinic.common.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiDraftService aiDraftService;
    private final AiVisitStructureService visitStructureService;
    private final AiInboundOcrService inboundOcrService;

    public AiController(AiDraftService aiDraftService,
                        AiVisitStructureService visitStructureService,
                        AiInboundOcrService inboundOcrService) {
        this.aiDraftService = aiDraftService;
        this.visitStructureService = visitStructureService;
        this.inboundOcrService = inboundOcrService;
    }

    @GetMapping("/status")
    public Result<AiStatusVO> status() {
        return Result.ok(aiDraftService.getStatus());
    }

    @GetMapping("/ocr/status")
    public Result<OcrStatusVO> ocrStatus() {
        return Result.ok(inboundOcrService.getStatus());
    }

    @PostMapping("/ocr/inbound")
    public Result<AiDraftVO> ocrInbound(@RequestParam("file") MultipartFile file) {
        return Result.ok(inboundOcrService.recognizeInbound(file));
    }

    @PostMapping("/structure/visit")
    public Result<AiDraftVO> structureVisit(@Valid @RequestBody StructureVisitRequest request) {
        return Result.ok(visitStructureService.structureVisit(request.text(), request.patientId()));
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

    @PutMapping("/drafts/{id}/payload")
    public Result<AiDraftVO> updateDraftPayload(
            @PathVariable Long id,
            @Valid @RequestBody UpdateAiDraftPayloadRequest request) {
        return Result.ok(aiDraftService.updatePayload(id, request));
    }

    @PostMapping("/drafts/{id}/approve-inbound")
    public Result<ApproveInboundResultVO> approveInbound(@PathVariable Long id) {
        return Result.ok(aiDraftService.approveInbound(id));
    }

    @PostMapping("/drafts/{id}/approve-visit")
    public Result<VisitDetailVO> approveVisit(
            @PathVariable Long id,
            @Valid @RequestBody ApproveVisitDraftRequest request) {
        return Result.ok(aiDraftService.approveVisit(id, request));
    }
}
