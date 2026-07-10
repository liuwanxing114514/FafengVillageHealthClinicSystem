package com.fafeng.clinic.common;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.ai.retry.NonTransientAiException;
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

    @ExceptionHandler(NonTransientAiException.class)
    @ResponseStatus(HttpStatus.OK)
    public Result<Void> handleNonTransientAi(NonTransientAiException ex) {
        org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class)
                .warn("AI upstream error: {}", ex.getMessage());
        String msg = ex.getMessage() != null ? ex.getMessage() : "";
        if (msg.contains("429") || msg.contains("rate limiting") || msg.contains("50609")) {
            return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, "AI 服务当前访问较多（限流），请稍后再试");
        }
        return Result.fail(ErrorCode.SERVICE_UNAVAILABLE, "AI 服务暂不可用，请稍后重试");
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
