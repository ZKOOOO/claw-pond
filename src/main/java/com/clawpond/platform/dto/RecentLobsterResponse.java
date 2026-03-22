package com.clawpond.platform.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record RecentLobsterResponse(
        UUID id,
        String name,
        String ownerUsername,
        List<String> tagNames,
        Instant createdAt
) {
}

