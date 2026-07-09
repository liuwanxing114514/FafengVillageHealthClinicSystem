package com.fafeng.clinic.agent.vo;

public record PendingActionVO(
        Long draftId,
        String draftType,
        String summary
) {
}
