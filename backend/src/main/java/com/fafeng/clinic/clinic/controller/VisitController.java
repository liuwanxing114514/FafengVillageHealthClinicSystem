package com.fafeng.clinic.clinic.controller;

import com.fafeng.clinic.clinic.dto.SaveVisitRequest;
import com.fafeng.clinic.clinic.dto.VisitSearchQuery;
import com.fafeng.clinic.clinic.service.VisitService;
import com.fafeng.clinic.clinic.vo.VisitDetailVO;
import com.fafeng.clinic.clinic.vo.VisitFeeSummaryVO;
import com.fafeng.clinic.clinic.vo.VisitListItemVO;
import com.fafeng.clinic.common.Result;
import com.fafeng.clinic.medicine.vo.PageVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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

/**
 * 门诊病历 CRUD 与就诊收费 API。
 */
@Tag(name = "病历", description = "门诊病历、应收实收与欠款筛选")
@RestController
@RequestMapping("/api/visits")
@RequiredArgsConstructor
public class VisitController {

    private final VisitService visitService;

    @Operation(summary = "病历列表", description = "支持关键词、日期范围、仅欠款筛选")
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

    @Operation(summary = "新建病历")
    @PostMapping
    public Result<VisitDetailVO> create(@Valid @RequestBody SaveVisitRequest request) {
        return Result.ok(visitService.create(request));
    }

    @Operation(summary = "病历详情")
    @GetMapping("/{id}")
    public Result<VisitDetailVO> detail(@PathVariable Long id) {
        return Result.ok(visitService.getDetail(id));
    }

    @Operation(summary = "收费摘要", description = "推荐应收、参考进货成本")
    @GetMapping("/{id}/fee-summary")
    public Result<VisitFeeSummaryVO> feeSummary(@PathVariable Long id) {
        return Result.ok(visitService.getFeeSummary(id));
    }

    @Operation(summary = "更新病历")
    @PutMapping("/{id}")
    public Result<VisitDetailVO> update(
            @PathVariable Long id,
            @Valid @RequestBody SaveVisitRequest request) {
        return Result.ok(visitService.update(id, request));
    }

    @Operation(summary = "软删除病历")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        visitService.delete(id);
        return Result.ok();
    }
}
