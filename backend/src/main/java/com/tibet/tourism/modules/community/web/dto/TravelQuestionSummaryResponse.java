package com.tibet.tourism.modules.community.web.dto;

import com.tibet.tourism.modules.community.domain.TravelQuestion;
import java.time.LocalDateTime;

public record TravelQuestionSummaryResponse(
        Long id,
        PublicUserResponse author,
        String title,
        String tags,
        Integer viewCount,
        Integer answerCount,
        Integer likeCount,
        Boolean isResolved,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TravelQuestionSummaryResponse fromEntity(TravelQuestion question) {
        return fromEntity(question, null);
    }

    public static TravelQuestionSummaryResponse fromEntity(TravelQuestion question, Long currentUserId) {
        if (question == null) {
            return null;
        }
        return new TravelQuestionSummaryResponse(
                question.getId(),
                PublicUserResponse.fromEntity(question.getAuthor(), currentUserId),
                question.getTitle(),
                question.getTags(),
                question.getViewCount(),
                question.getAnswerCount(),
                question.getLikeCount(),
                question.getIsResolved(),
                question.getCreatedAt(),
                question.getUpdatedAt());
    }
}
