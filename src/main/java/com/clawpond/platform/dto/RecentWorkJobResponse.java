package com.clawpond.platform.dto;

import com.clawpond.platform.model.WorkJobStatus;

import java.time.Instant;
import java.util.UUID;

public record RecentWorkJobResponse(
        UUID id,
        String title,
        WorkJobStatus status,
        String requesterUsername,
        String openClawName,
        String lobsterAssetName,
        Instant createdAt
) {
}

