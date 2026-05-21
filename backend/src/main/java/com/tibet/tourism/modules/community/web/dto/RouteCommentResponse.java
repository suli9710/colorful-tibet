package com.tibet.tourism.modules.community.web.dto;

import com.tibet.tourism.modules.community.domain.RouteComment;
import java.time.LocalDateTime;

public record RouteCommentResponse(
        Long id,
        PublicUserResponse user,
        String content,
        LocalDateTime createdAt
) {
    public static RouteCommentResponse fromEntity(RouteComment comment) {
        if (comment == null) {
            return null;
        }
        return new RouteCommentResponse(
                comment.getId(),
                PublicUserResponse.fromEntity(comment.getUser()),
                comment.getContent(),
                comment.getCreatedAt());
    }
}
