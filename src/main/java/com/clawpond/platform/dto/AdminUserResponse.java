package com.clawpond.platform.dto;

import com.clawpond.platform.model.Role;

import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String username,
        String email,
        Role role,
        boolean enabled,
        Instant createdAt
) {
}

