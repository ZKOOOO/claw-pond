package com.clawpond.platform.dto;

import com.clawpond.platform.model.Role;

import java.time.Instant;
import java.util.UUID;

public record AuthResponse(
        String token,
        UUID userId,
        String username,
        String email,
        Role role,
        Instant createdAt
) {
}

