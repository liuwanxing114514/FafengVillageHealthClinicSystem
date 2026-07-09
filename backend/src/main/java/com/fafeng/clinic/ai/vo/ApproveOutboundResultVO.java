package com.fafeng.clinic.ai.vo;

public record ApproveOutboundResultVO(
        Long prescriptionId,
        int lineCount,
        int flowCount
) {
}
