package com.fafeng.clinic.ai.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record ReorderChannelsRequest(
        @NotEmpty List<String> channelIds) {
}
