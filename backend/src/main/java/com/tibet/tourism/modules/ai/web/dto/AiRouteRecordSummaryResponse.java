package com.tibet.tourism.modules.ai.web.dto;

import java.time.LocalDateTime;

public record AiRouteRecordSummaryResponse(
        Long id,
        String title,
        Integer days,
        String budget,
        String preference,
        String locale,
        String status,
        Boolean manuallySaved,
        String errorMessage,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
