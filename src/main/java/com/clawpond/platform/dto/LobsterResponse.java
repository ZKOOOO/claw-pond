package com.clawpond.platform.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LobsterResponse(
        UUID id,
        String name,
        String description,
        String originalFilename,
        String downloadUrl,
        List<String> tagNames,
        Instant createdAt
) {
}
