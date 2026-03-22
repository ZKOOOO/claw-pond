package com.clawpond.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * 创建任务单请求。
 */
public record WorkJobCreateRequest(
        @NotBlank @Size(min = 2, max = 120) String title,
        @Size(max = 1000) String description,
        @NotNull UUID openClawId,
        UUID lobsterAssetId,
        List<@Size(min = 1, max = 32) String> desiredTags
) {
}
