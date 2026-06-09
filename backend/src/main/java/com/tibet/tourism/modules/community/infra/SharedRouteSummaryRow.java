package com.tibet.tourism.modules.community.infra;

import com.tibet.tourism.modules.community.domain.SharedRoute;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SharedRouteSummaryRow(
        Long id,
        Long authorId,
        String authorUsername,
        String authorNickname,
        String authorAvatar,
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
}
