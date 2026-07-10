package com.fafeng.clinic.ai.client;

import com.fafeng.clinic.ai.channel.AiChannelFailoverPolicy;
import com.fafeng.clinic.common.BusinessException;
import com.fafeng.clinic.common.ErrorCode;

/**
 * 将 Spring AI / 上游 HTTP 异常转为对用户友好的业务异常。
 */
public final class AiClientExceptionMapper {

    private AiClientExceptionMapper() {
    }

    public static BusinessException toBusinessException(Exception ex) {
        String msg = messageOf(ex);
        if (isRateLimited(msg)) {
            return new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
                    "AI 服务当前较忙（DeepSeek 限流/拥堵），请稍后再试");
        }
        if (containsAny(msg, "401", "403", "invalid api key", "authentication")) {
            return new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
                    "AI API Key 无效或已过期，请检查配置");
        }
        if (containsAny(msg, "404", "model not found")) {
            return new BusinessException(ErrorCode.SERVICE_UNAVAILABLE,
                    "AI 模型不可用，请检查模型配置");
        }
        return new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 服务暂不可用，请稍后重试");
    }

    /** 主通道失败时是否值得切换备用 API（限流/拥堵/短暂不可用）。 */
    public static boolean isFallbackEligible(Exception ex) {
        return AiChannelFailoverPolicy.isFallbackEligible(ex);
    }

    private static boolean isRateLimited(String msg) {
        return containsAny(msg, "429", "rate limiting", "50609", "50508", "too busy", "system is too busy");
    }

    private static String messageOf(Exception ex) {
        return ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
    }

    private static boolean containsAny(String msg, String... tokens) {
        String lower = msg.toLowerCase();
        for (String token : tokens) {
            if (lower.contains(token.toLowerCase())) {
                return true;
            }
        }
        return false;
    }
}
