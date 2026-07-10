package com.fafeng.clinic.ai.vo;

public record ExternalServiceItemVO(
        String serviceCode,
        boolean enabled,
        String endpointUrl,
        boolean configured,
        int channelCount) {
}
