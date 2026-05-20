package com.tibet.tourism.modules.community.web.dto;

import com.tibet.tourism.modules.community.domain.TravelAnswer;
import java.time.LocalDateTime;

public record TravelAnswerResponse(
        Long id,
        PublicUserResponse user,
        String content,
        Integer likeCount,
        Boolean isAccepted,
        LocalDateTime createdAt
) {
    public static TravelAnswerResponse fromEntity(TravelAnswer answer) {
        if (answer == null) {
            return null;
        }
        return new TravelAnswerResponse(
                answer.getId(),
                PublicUserResponse.fromEntity(answer.getUser()),
                answer.getContent(),
                answer.getLikeCount(),
                answer.getIsAccepted(),
                answer.getCreatedAt());
    }
}
