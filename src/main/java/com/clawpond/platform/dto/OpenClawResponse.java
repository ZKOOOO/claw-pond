package com.clawpond.platform.dto;

import java.time.Instant;
import java.util.UUID;

public record OpenClawResponse(
        UUID id,
        String name,
        String baseUrl,
        String externalId,
        String description,
        boolean active,
        String ownerUsername,
        Instant createdAt
) {
}

