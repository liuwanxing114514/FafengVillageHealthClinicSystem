package com.fafeng.clinic.clinic.controller;

import com.fafeng.clinic.clinic.dto.SaveVisitRequest;
import com.fafeng.clinic.clinic.service.VisitService;
import com.fafeng.clinic.clinic.vo.VisitDetailVO;
import com.fafeng.clinic.common.Result;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/visits")
public class VisitController {

    private final VisitService visitService;

    public VisitController(VisitService visitService) {
        this.visitService = visitService;
    }

    @PostMapping
    public Result<VisitDetailVO> create(@Valid @RequestBody SaveVisitRequest request) {
        return Result.ok(visitService.create(request));
    }

    @GetMapping("/{id}")
    public Result<VisitDetailVO> detail(@PathVariable Long id) {
        return Result.ok(visitService.getDetail(id));
    }

    @PutMapping("/{id}")
    public Result<VisitDetailVO> update(
            @PathVariable Long id,
            @Valid @RequestBody SaveVisitRequest request) {
        return Result.ok(visitService.update(id, request));
    }
}
