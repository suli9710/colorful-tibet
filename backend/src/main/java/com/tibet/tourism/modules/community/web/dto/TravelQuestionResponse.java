package com.tibet.tourism.modules.community.web.dto;

import com.tibet.tourism.modules.community.domain.TravelQuestion;
import java.time.LocalDateTime;

public record TravelQuestionResponse(
        Long id,
        PublicUserResponse author,
        String title,
        String content,
        String tags,
        Integer viewCount,
        Integer answerCount,
        Integer likeCount,
        Boolean isResolved,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TravelQuestionResponse fromEntity(TravelQuestion question) {
        return fromEntity(question, null);
    }

    public static TravelQuestionResponse fromEntity(TravelQuestion question, Long currentUserId) {
        if (question == null) {
            return null;
        }
        return new TravelQuestionResponse(
                question.getId(),
                PublicUserResponse.fromEntity(question.getAuthor(), currentUserId),
                question.getTitle(),
                question.getContent(),
                question.getTags(),
                question.getViewCount(),
                question.getAnswerCount(),
                question.getLikeCount(),
                question.getIsResolved(),
                question.getCreatedAt(),
                question.getUpdatedAt());
    }
}
