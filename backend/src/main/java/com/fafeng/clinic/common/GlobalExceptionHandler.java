package com.fafeng.clinic.common;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.ai.retry.NonTransientAiException;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({BadCredentialsException.class, AuthenticationException.class})
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleAuth(AuthenticationException ex) {
        return Result.fail(ErrorCode.UNAUTHORIZED, "密码错误");
    }

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleBusiness(BusinessException ex) {
        return Result.fail(ex.getErrorCode(), ex.getMessage());
    }

    @ExceptionHandler({NonTransientAiException.class, TransientAiException.class})
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleAiUpstream(Exception ex) {
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class)
                .warn("AI upstream error: {}", ex.getMessage());
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, mapAiUpstreamMessage(ex.getMessage()));
    }

    private static String mapAiUpstreamMessage(String raw) {
        String msg = raw != null ? raw : "";
        String lower = msg.toLowerCase();
        if (lower.contains("429") || lower.contains("rate limiting")
                || lower.contains("50609") || lower.contains("50508")
                || lower.contains("too busy") || lower.contains("system is too busy")) {
            return "AI 服务当前较忙（DeepSeek 限流/拥堵），请稍后再试";
        }
        if (lower.contains("503") || lower.contains("502") || lower.contains("504")) {
            return "AI 服务暂时不可用，请稍后重试";
        }
        if (lower.contains("401") || lower.contains("403") || lower.contains("invalid api key")) {
            return "AI API Key 无效或已过期，请检查系统设置";
        }
        return "AI 服务暂不可用，请稍后重试";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("；"));
        return Result.fail(ErrorCode.BAD_REQUEST, message);
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleUnknown(Exception ex) {
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class)
                .error("Unhandled exception", ex);
        return Result.fail(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.message());
    }
}
