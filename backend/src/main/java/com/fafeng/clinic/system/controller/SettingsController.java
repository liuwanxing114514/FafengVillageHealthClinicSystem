package com.fafeng.clinic.system.controller;

import com.fafeng.clinic.common.Result;
import com.fafeng.clinic.system.dto.UpdateSettingRequest;
import com.fafeng.clinic.system.service.SettingsService;
import com.fafeng.clinic.system.vo.SettingVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public Result<List<SettingVO>> list() {
        return Result.ok(settingsService.listAll());
    }

    @PutMapping("/{key}")
    public Result<SettingVO> update(
            @PathVariable String key,
            @Valid @RequestBody UpdateSettingRequest request) {
        return Result.ok(settingsService.update(key, request));
    }
}
