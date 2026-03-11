package com.clawpond.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OpenClawRegistrationRequest(
        @NotBlank @Size(min = 2, max = 100) String name,
        @NotBlank
        @Pattern(regexp = "^https?://\\S+$", message = "baseUrl must start with http:// or https:// and must not contain spaces")
        String baseUrl,
        @NotBlank @Size(min = 3, max = 120) String externalId,
        @Size(max = 500) String description,
        @Size(max = 255) String apiToken
) {
}
