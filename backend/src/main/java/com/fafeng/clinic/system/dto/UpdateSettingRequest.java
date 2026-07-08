package com.fafeng.clinic.system.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateSettingRequest(
        @NotBlank(message = "设置值不能为空") String value
) {
}
