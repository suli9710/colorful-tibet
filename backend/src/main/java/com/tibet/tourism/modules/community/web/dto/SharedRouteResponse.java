package com.tibet.tourism.modules.community.web.dto;

import com.tibet.tourism.modules.community.domain.SharedRoute;
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
        LocalDateTime updatedAt
) {
    public static SharedRouteResponse fromEntity(SharedRoute route) {
        if (route == null) {
            return null;
        }
        return new SharedRouteResponse(
                route.getId(),
                PublicUserResponse.fromEntity(route.getAuthor()),
                route.getTitle(),
                route.getContent(),
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
