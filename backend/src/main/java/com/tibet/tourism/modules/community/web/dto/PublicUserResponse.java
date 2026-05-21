package com.tibet.tourism.modules.community.web.dto;

import com.tibet.tourism.modules.user.domain.User;

public record PublicUserResponse(
        Long id,
        String nickname,
        String avatar
) {
    public static PublicUserResponse fromEntity(User user) {
        if (user == null) {
            return null;
        }
        return new PublicUserResponse(
                user.getId(),
                user.getNickname(),
                user.getAvatar());
    }
}
