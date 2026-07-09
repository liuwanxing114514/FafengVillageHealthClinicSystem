package com.fafeng.clinic.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record BatchOutboundPreviewRequest(
        @NotEmpty @Valid List<OutboundLineRequest> items
) {
}
