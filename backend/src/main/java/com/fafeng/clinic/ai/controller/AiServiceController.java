package com.fafeng.clinic.ai.controller;

import com.fafeng.clinic.ai.dto.UpdateExternalServiceRequest;
import com.fafeng.clinic.ai.service.ExternalServiceService;
import com.fafeng.clinic.ai.vo.ExternalServiceItemVO;
import com.fafeng.clinic.ai.vo.ExternalServicesOverviewVO;
import com.fafeng.clinic.common.Result;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai/services")
@RequiredArgsConstructor
public class AiServiceController {

    private final ExternalServiceService externalServiceService;

    @GetMapping
    public Result<ExternalServicesOverviewVO> list() {
        return Result.ok(externalServiceService.getOverview());
    }

    @PutMapping("/{code}")
    public Result<ExternalServiceItemVO> update(
            @PathVariable String code,
            @Valid @RequestBody UpdateExternalServiceRequest request) {
        return Result.ok(externalServiceService.updateService(code, request));
    }
}
