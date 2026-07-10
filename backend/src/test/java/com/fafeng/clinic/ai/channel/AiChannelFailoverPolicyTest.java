package com.fafeng.clinic.ai.channel;

import org.junit.jupiter.api.Test;
import org.springframework.ai.retry.NonTransientAiException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiChannelFailoverPolicyTest {

    @Test
    void eligibleForRateLimitAnd503() {
        assertTrue(AiChannelFailoverPolicy.isFallbackEligible(
                new NonTransientAiException("429 - rate limiting")));
        assertTrue(AiChannelFailoverPolicy.isFallbackEligible(
                new NonTransientAiException("503 Service Unavailable")));
        assertTrue(AiChannelFailoverPolicy.isFallbackEligible(
                new NonTransientAiException("connection timed out")));
    }

    @Test
    void notEligibleFor401And403() {
        assertFalse(AiChannelFailoverPolicy.isFallbackEligible(
                new NonTransientAiException("401 Unauthorized")));
        assertFalse(AiChannelFailoverPolicy.isFallbackEligible(
                new NonTransientAiException("403 invalid api key")));
    }
}
