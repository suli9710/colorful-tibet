package com.tibet.tourism.modules.ai.application;

import java.time.Instant;

public record AiRouteJobSnapshot(
        String jobId,
        Long routeRecordId,
        String status,
        String content,
        String errorMessage,
        int days,
        String budget,
        String preference,
        boolean cached,
        Instant createdAt,
        Instant updatedAt
) {
}
