package com.fafeng.clinic.clinic.controller;

import com.fafeng.clinic.clinic.dto.SavePrescriptionRequest;
import com.fafeng.clinic.clinic.service.PrescriptionService;
import com.fafeng.clinic.clinic.vo.OutboundDraftVO;
import com.fafeng.clinic.clinic.vo.PrescriptionDetailVO;
import com.fafeng.clinic.clinic.vo.PrescriptionPrintVO;
import com.fafeng.clinic.common.Result;
import jakarta.validation.Valid;
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

@RestController
@RequestMapping("/api/prescriptions")
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    public PrescriptionController(PrescriptionService prescriptionService) {
        this.prescriptionService = prescriptionService;
    }

    @PostMapping
    public Result<PrescriptionDetailVO> create(@Valid @RequestBody SavePrescriptionRequest request) {
        return Result.ok(prescriptionService.create(request));
    }

    @GetMapping("/{id}")
    public Result<PrescriptionDetailVO> detail(@PathVariable Long id) {
        return Result.ok(prescriptionService.getDetail(id));
    }

    @PutMapping("/{id}")
    public Result<PrescriptionDetailVO> update(
            @PathVariable Long id,
            @Valid @RequestBody SavePrescriptionRequest request) {
        return Result.ok(prescriptionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> voidPrescription(@PathVariable Long id) {
        prescriptionService.voidPrescription(id);
        return Result.ok();
    }

    @GetMapping("/{id}/print")
    public Result<PrescriptionPrintVO> print(@PathVariable Long id) {
        return Result.ok(prescriptionService.getPrintData(id));
    }

    @PostMapping("/{id}/outbound-draft")
    public Result<OutboundDraftVO> outboundDraft(@PathVariable Long id) {
        return Result.ok(prescriptionService.generateOutboundDraft(id));
    }

    @GetMapping
    public Result<List<PrescriptionDetailVO>> listByVisit(@RequestParam Long visitId) {
        return Result.ok(prescriptionService.listByVisit(visitId));
    }
}
