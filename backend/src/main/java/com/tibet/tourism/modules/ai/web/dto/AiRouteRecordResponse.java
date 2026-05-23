package com.tibet.tourism.modules.ai.web.dto;

import com.tibet.tourism.modules.ai.domain.AiRouteRecord;
import java.time.LocalDateTime;

public record AiRouteRecordResponse(
        Long id,
        String jobId,
        String title,
        String content,
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
    public static AiRouteRecordResponse from(AiRouteRecord record) {
        return new AiRouteRecordResponse(
                record.getId(),
                record.getJobId(),
                record.getTitle(),
                record.getContent(),
                record.getDays(),
                record.getBudget(),
                record.getPreference(),
                record.getLocale(),
                record.getStatus() == null ? null : record.getStatus().name(),
                Boolean.TRUE.equals(record.getManuallySaved()),
                record.getErrorMessage(),
                record.getCreatedAt(),
                record.getUpdatedAt()
        );
    }
}
