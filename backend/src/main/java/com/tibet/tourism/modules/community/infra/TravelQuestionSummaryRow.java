package com.tibet.tourism.modules.community.infra;

import java.time.LocalDateTime;

public record TravelQuestionSummaryRow(
        Long id,
        Long authorId,
        String authorUsername,
        String authorNickname,
        String authorAvatar,
        String title,
        String excerpt,
        String tags,
        Integer viewCount,
        Integer answerCount,
        Integer likeCount,
        Boolean isResolved,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
