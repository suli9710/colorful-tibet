package com.tibet.tourism.modules.ai.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.tibet.tourism.modules.ai.domain.AiRouteRecord;
import java.time.LocalDateTime;

public record AiRouteRecordResponse(
        Long id,
        @JsonInclude(JsonInclude.Include.NON_NULL)
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
        return from(record, false);
    }

    public static AiRouteRecordResponse fromLatest(AiRouteRecord record) {
        return from(record, record.getStatus() == AiRouteRecord.Status.RUNNING);
    }

    private static AiRouteRecordResponse from(AiRouteRecord record, boolean exposeJobId) {
        return new AiRouteRecordResponse(
                record.getId(),
                exposeJobId ? record.getJobId() : null,
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
