package com.clawpond.platform.dto;

import java.util.List;

public record AdminOverviewResponse(
        long totalUsers,
        long totalOpenClaws,
        long activeOpenClaws,
        long totalWorkJobs,
        long totalLobsters,
        long totalTags,
        List<RecentUserResponse> recentUsers,
        List<RecentOpenClawResponse> recentOpenClaws,
        List<RecentWorkJobResponse> recentWorkJobs,
        List<RecentLobsterResponse> recentLobsters
) {
}

