package com.clawpond.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * OpenClaw 实例更新请求。
 */
public record OpenClawUpdateRequest(
        @NotBlank @Size(min = 2, max = 100) String name,
        @NotBlank
        @Pattern(regexp = "^https?://\\S+$", message = "访问地址必须以 http:// 或 https:// 开头，且不能包含空格")
        String baseUrl,
        @NotBlank @Size(min = 3, max = 120) String externalId,
        @Size(max = 500) String description,
        @Size(max = 255) String apiToken,
        List<@Size(min = 1, max = 32) String> tagNames,
        @NotNull Boolean active
) {
}
