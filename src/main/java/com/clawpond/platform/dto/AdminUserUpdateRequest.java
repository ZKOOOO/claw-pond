package com.clawpond.platform.dto;

import com.clawpond.platform.model.Role;
import jakarta.validation.constraints.NotNull;

public record AdminUserUpdateRequest(
        @NotNull Role role,
        @NotNull Boolean enabled
) {
}
