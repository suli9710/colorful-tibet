package com.tibet.tourism.modules.community.web.dto;

import com.tibet.tourism.modules.community.domain.SharedRoute;
import com.tibet.tourism.common.security.PublicTravelText;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SharedRouteSummaryResponse(
        Long id,
        PublicUserResponse author,
        String title,
        Integer days,
        String budget,
        String preference,
        SharedRoute.SourceType sourceType,
        Long sourceRouteId,
        BigDecimal price,
        String difficulty,
        String temperature,
        String geography,
        Integer viewCount,
        Integer likeCount,
        Integer commentCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    @com.fasterxml.jackson.annotation.JsonProperty("verificationStatus")
    public String verificationStatus() {
        return sourceType == SharedRoute.SourceType.USER ? "SELF_REPORTED" : "NOT_INDEPENDENTLY_VERIFIED";
    }

    public static SharedRouteSummaryResponse fromEntity(SharedRoute route) {
        return fromEntity(route, null);
    }

    public static SharedRouteSummaryResponse fromEntity(SharedRoute route, Long currentUserId) {
        if (route == null) {
            return null;
        }
        return new SharedRouteSummaryResponse(
                route.getId(),
                PublicUserResponse.fromEntity(route.getAuthor(), currentUserId),
                PublicTravelText.minimize(route.getTitle()),
                route.getDays(),
                route.getBudget(),
                route.getPreference(),
                route.getSourceType(),
                route.getSourceRouteId(),
                route.getPrice(),
                route.getDifficulty(),
                route.getTemperature(),
                route.getGeography(),
                route.getViewCount(),
                route.getLikeCount(),
                route.getCommentCount(),
                route.getCreatedAt(),
                route.getUpdatedAt());
    }
}
