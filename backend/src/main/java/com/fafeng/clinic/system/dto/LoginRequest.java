package com.fafeng.clinic.system.dto;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank(message = "密码不能为空") String password
) {
}
