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
    @com.fasterxml.jackson.annotation.JsonProperty("trustStatus")
    public String trustStatus() {
        return com.tibet.tourism.modules.route.application.ItineraryTrustPolicy.violatesOvernightPolicy(content)
                ? "BLOCKED" : "NEEDS_CONFIRMATION";
    }
    @com.fasterxml.jackson.annotation.JsonProperty("verificationNotice")
    public String verificationNotice() {
        return "历史生成内容与保存状态不等于路线已核验；请重新确认出行日期、预约、交通、住宿和报价。";
    }
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
