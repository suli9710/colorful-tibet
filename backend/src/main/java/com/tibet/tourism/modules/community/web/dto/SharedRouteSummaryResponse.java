package com.tibet.tourism.modules.community.web.dto;

import com.tibet.tourism.modules.community.domain.SharedRoute;
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
                route.getTitle(),
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
