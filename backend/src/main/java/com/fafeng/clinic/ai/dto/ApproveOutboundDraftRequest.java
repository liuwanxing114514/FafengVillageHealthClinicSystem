package com.fafeng.clinic.ai.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ApproveOutboundDraftRequest(
        @NotEmpty @Valid List<ApproveOutboundLineRequest> lines
) {
}
