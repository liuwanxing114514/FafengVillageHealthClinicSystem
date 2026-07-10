package com.fafeng.clinic.ai.channel;

/**
 * Chat / Embedding 多通道 failover 策略：429/503/timeout 可切换，401/403 不切换。
 */
public final class AiChannelFailoverPolicy {

    private AiChannelFailoverPolicy() {
    }

    public static boolean isFallbackEligible(Exception ex) {
        String msg = messageOf(ex);
        if (containsAny(msg, "401", "403", "invalid api key", "authentication")) {
            return false;
        }
        return isRateLimited(msg)
                || containsAny(msg, "502", "503", "504", "timeout", "timed out", "connection reset", "connection refused");
    }

    private static boolean isRateLimited(String msg) {
        return containsAny(msg, "429", "rate limiting", "50609", "too busy");
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
