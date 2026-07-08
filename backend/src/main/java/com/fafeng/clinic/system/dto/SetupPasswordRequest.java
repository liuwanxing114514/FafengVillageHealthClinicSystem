package com.fafeng.clinic.system.dto;

import jakarta.validation.constraints.NotBlank;

public record SetupPasswordRequest(
        @NotBlank(message = "密码不能为空") String password,
        @NotBlank(message = "请确认密码") String confirmPassword
) {
}
