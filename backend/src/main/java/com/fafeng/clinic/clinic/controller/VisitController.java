package com.fafeng.clinic.clinic.controller;

import com.fafeng.clinic.clinic.dto.SaveVisitRequest;
import com.fafeng.clinic.clinic.dto.VisitSearchQuery;
import com.fafeng.clinic.clinic.service.VisitService;
import com.fafeng.clinic.clinic.vo.VisitDetailVO;
import com.fafeng.clinic.clinic.vo.VisitFeeSummaryVO;
import com.fafeng.clinic.clinic.vo.VisitListItemVO;
import com.fafeng.clinic.common.Result;
import com.fafeng.clinic.medicine.vo.PageVO;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/visits")
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    @GetMapping
    public Result<PageVO<VisitListItemVO>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(required = false) Boolean arrearsOnly,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        VisitSearchQuery query = new VisitSearchQuery(keyword, dateFrom, dateTo, arrearsOnly);
        return Result.ok(visitService.search(query, page, size));
    }

    @PostMapping
    public Result<VisitDetailVO> create(@Valid @RequestBody SaveVisitRequest request) {
        return Result.ok(visitService.create(request));
    }

    @GetMapping("/{id}")
    public Result<VisitDetailVO> detail(@PathVariable Long id) {
        return Result.ok(visitService.getDetail(id));
    }

    @GetMapping("/{id}/fee-summary")
    public Result<VisitFeeSummaryVO> feeSummary(@PathVariable Long id) {
        return Result.ok(visitService.getFeeSummary(id));
    }

    @PutMapping("/{id}")
    public Result<VisitDetailVO> update(
            @PathVariable Long id,
            @Valid @RequestBody SaveVisitRequest request) {
        return Result.ok(visitService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        visitService.delete(id);
        return Result.ok();
    }
}
