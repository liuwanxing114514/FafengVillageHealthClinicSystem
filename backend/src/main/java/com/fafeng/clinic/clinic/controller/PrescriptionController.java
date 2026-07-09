package com.fafeng.clinic.clinic.controller;

import com.fafeng.clinic.clinic.dto.SavePrescriptionRequest;
import com.fafeng.clinic.clinic.service.PrescriptionService;
import com.fafeng.clinic.clinic.vo.OutboundDraftVO;
import com.fafeng.clinic.clinic.vo.PrescriptionDetailVO;
import com.fafeng.clinic.clinic.vo.PrescriptionPrintVO;
import com.fafeng.clinic.common.Result;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 处方 CRUD、打印与待出库草稿 API。
 * <p>出库草稿仅生成待确认数据，不直接扣减库存。</p>
 */
@Tag(name = "处方", description = "处方记录、打印与出库草稿")
@RestController
@RequestMapping("/api/prescriptions")
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @Operation(summary = "新建处方")
    @PostMapping
    public Result<PrescriptionDetailVO> create(@Valid @RequestBody SavePrescriptionRequest request) {
        return Result.ok(prescriptionService.create(request));
    }

    @Operation(summary = "处方详情")
    @GetMapping("/{id}")
    public Result<PrescriptionDetailVO> detail(@PathVariable Long id) {
        return Result.ok(prescriptionService.getDetail(id));
    }

    @Operation(summary = "更新处方")
    @PutMapping("/{id}")
    public Result<PrescriptionDetailVO> update(
            @PathVariable Long id,
            @Valid @RequestBody SavePrescriptionRequest request) {
        return Result.ok(prescriptionService.update(id, request));
    }

    @Operation(summary = "作废处方")
    @DeleteMapping("/{id}")
    public Result<Void> voidPrescription(@PathVariable Long id) {
        prescriptionService.voidPrescription(id);
        return Result.ok();
    }

    @Operation(summary = "打印数据", description = "A4 或预印纸模板渲染数据")
    @GetMapping("/{id}/print")
    public Result<PrescriptionPrintVO> print(@PathVariable Long id) {
        return Result.ok(prescriptionService.getPrintData(id));
    }

    @Operation(summary = "生成待出库草稿", description = "写入 ai_draft，不扣库存")
    @PostMapping("/{id}/outbound-draft")
    public Result<OutboundDraftVO> outboundDraft(@PathVariable Long id) {
        return Result.ok(prescriptionService.generateOutboundDraft(id));
    }

    @Operation(summary = "按病历列出处方")
    @GetMapping
    public Result<List<PrescriptionDetailVO>> listByVisit(@RequestParam Long visitId) {
        return Result.ok(prescriptionService.listByVisit(visitId));
    }
}
