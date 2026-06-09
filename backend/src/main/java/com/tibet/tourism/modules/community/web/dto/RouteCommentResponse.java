package com.tibet.tourism.modules.community.web.dto;

import com.tibet.tourism.modules.community.domain.RouteComment;
import java.time.LocalDateTime;

public record RouteCommentResponse(
        Long id,
        PublicUserResponse user,
        ParentRouteResponse route,
        String content,
        LocalDateTime createdAt
) {
    public static RouteCommentResponse fromEntity(RouteComment comment) {
        return fromEntity(comment, null);
    }

    public static RouteCommentResponse fromEntity(RouteComment comment, Long currentUserId) {
        if (comment == null) {
            return null;
        }
        return new RouteCommentResponse(
                comment.getId(),
                PublicUserResponse.fromEntity(comment.getUser(), currentUserId),
                comment.getRoute() == null
                        ? null
                        : new ParentRouteResponse(comment.getRoute().getId(), comment.getRoute().getTitle()),
                comment.getContent(),
                comment.getCreatedAt());
    }

    public record ParentRouteResponse(Long id, String title) {}
}
