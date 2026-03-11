package com.clawpond.platform.dto;

import com.clawpond.platform.model.WorkJobStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record WorkJobResponse(
        UUID id,
        String title,
        String description,
        WorkJobStatus status,
        UUID openClawId,
        String openClawName,
        List<String> openClawTags,
        List<String> desiredTags,
        Instant createdAt
) {
}

