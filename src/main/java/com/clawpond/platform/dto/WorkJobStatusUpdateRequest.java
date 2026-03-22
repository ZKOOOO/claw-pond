package com.clawpond.platform.dto;

import com.clawpond.platform.model.WorkJobStatus;
import jakarta.validation.constraints.NotNull;

public record WorkJobStatusUpdateRequest(
        @NotNull WorkJobStatus status
) {
}

