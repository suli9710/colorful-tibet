package com.tibet.tourism.modules.community.web.dto;

import com.tibet.tourism.modules.community.domain.SharedRoute;
import com.tibet.tourism.common.security.PublicTravelText;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SharedRouteResponse(
        Long id,
        PublicUserResponse author,
        String title,
        String content,
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
        LocalDateTime updatedAt,
        String travelStatus,
        String travelMonth,
        String verificationStatus
) {
    public static SharedRouteResponse fromEntity(SharedRoute route) {
        return fromEntity(route, null);
    }

    public static SharedRouteResponse fromEntity(SharedRoute route, Long currentUserId) {
        if (route == null) {
            return null;
        }
        return new SharedRouteResponse(
                route.getId(),
                PublicUserResponse.fromEntity(route.getAuthor(), currentUserId),
                PublicTravelText.minimize(route.getTitle()),
                PublicTravelText.minimize(route.getContent()),
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
                route.getUpdatedAt(),
                route.getTravelStatus() == null ? "UNKNOWN" : route.getTravelStatus(),
                route.getTravelMonth(),
                route.getSourceType() == SharedRoute.SourceType.USER ? "SELF_REPORTED" : "NOT_INDEPENDENTLY_VERIFIED");
    }
}
