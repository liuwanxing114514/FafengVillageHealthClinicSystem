
package com.fafeng.clinic.system.controller;

import com.fafeng.clinic.common.Result;
import com.fafeng.clinic.system.dto.ChangePasswordRequest;
import com.fafeng.clinic.system.dto.LoginRequest;
import com.fafeng.clinic.system.service.AuthService;
import com.fafeng.clinic.system.vo.SessionVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;


    @PostMapping("/login")
    public Result<Void> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        authService.login(request, httpRequest);
        return Result.ok();
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest httpRequest) {
        authService.logout(httpRequest);
        return Result.ok();
    }

    @PostMapping("/change-password")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return Result.ok();
    }

    @GetMapping("/session")
    public Result<SessionVO> session() {
        return Result.ok(authService.currentSession());
    }
}
