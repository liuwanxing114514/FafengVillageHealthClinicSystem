package com.fafeng.clinic.ai.vo;

public record AiStatusVO(
        boolean enabled,
        String provider,
        boolean providerAvailable
) {
}
