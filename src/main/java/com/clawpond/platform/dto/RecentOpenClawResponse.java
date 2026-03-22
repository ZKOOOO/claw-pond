package com.clawpond.platform.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecentOpenClawResponse(
        UUID id,
        String name,
        String ownerUsername,
        boolean active,
        List<String> tagNames,
        Instant createdAt
) {
}

