
package com.fafeng.clinic.system.controller;

import com.fafeng.clinic.common.Result;
import com.fafeng.clinic.system.dto.SetupPasswordRequest;
import com.fafeng.clinic.system.service.SystemSetupService;
import com.fafeng.clinic.system.vo.SetupStatusVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final SystemSetupService systemSetupService;


    @GetMapping("/setup-status")
    public Result<SetupStatusVO> setupStatus() {
        return Result.ok(systemSetupService.getSetupStatus());
    }

    @PostMapping("/setup-password")
    public Result<Void> setupPassword(@Valid @RequestBody SetupPasswordRequest request) {
        systemSetupService.setupPassword(request);
        return Result.ok();
    }
}
