package com.tibet.tourism.modules.community.web.dto;

import com.tibet.tourism.modules.user.domain.User;
import org.springframework.util.StringUtils;

public record PublicUserResponse(
        String nickname,
        String avatar,
        boolean owner
) {
    public static PublicUserResponse fromEntity(User user) {
        return fromEntity(user, null);
    }

    public static PublicUserResponse fromEntity(User user, Long currentUserId) {
        if (user == null) {
            return null;
        }
        return new PublicUserResponse(
                publicNickname(user),
                user.getAvatar(),
                currentUserId != null && currentUserId.equals(user.getId()));
    }

    static String publicNickname(User user) {
        String nickname = user.getNickname();
        if (!StringUtils.hasText(nickname)) {
            return null;
        }
        String normalizedNickname = nickname.trim();
        String username = user.getUsername();
        if (StringUtils.hasText(username) && normalizedNickname.equalsIgnoreCase(username.trim())) {
            return null;
        }
        return normalizedNickname;
    }
}
