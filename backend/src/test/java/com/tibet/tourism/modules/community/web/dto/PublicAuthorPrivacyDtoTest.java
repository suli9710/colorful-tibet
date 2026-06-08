package com.tibet.tourism.modules.community.web.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.tibet.tourism.modules.community.domain.Comment;
import com.tibet.tourism.modules.user.domain.User;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class PublicAuthorPrivacyDtoTest {

    @Test
    void publicUserResponseHidesNicknameWhenItDefaultsToUsername() {
        User user = user("login-name", " LOGIN-NAME ");

        PublicUserResponse response = PublicUserResponse.fromEntity(user, 7L);

        assertThat(response.nickname()).isNull();
        assertThat(response.avatar()).isEqualTo("/avatars/u7.png");
        assertThat(response.owner()).isTrue();
    }

    @Test
    void publicUserResponseHidesBlankNickname() {
        User user = user("login-name", "   ");

        PublicUserResponse response = PublicUserResponse.fromEntity(user, null);

        assertThat(response.nickname()).isNull();
        assertThat(response.owner()).isFalse();
    }

    @Test
    void publicUserResponseKeepsExplicitNicknameDifferentFromUsername() {
        User user = user("login-name", " Public Nickname ");

        PublicUserResponse response = PublicUserResponse.fromEntity(user, 42L);

        assertThat(response.nickname()).isEqualTo("Public Nickname");
        assertThat(response.owner()).isFalse();
    }

    @Test
    void commentDtoUsesSafePublicNicknameForFlatAuthorFields() {
        Comment comment = new Comment();
        comment.setId(10L);
        comment.setUser(user("login-name", "login-name"));
        comment.setContent("Nice view");
        comment.setRating(5);
        comment.setImageUrl("/uploads/comments/photo.jpg");
        comment.setLikeCount(3);
        comment.setCreatedAt(LocalDateTime.parse("2026-01-02T03:04:05"));

        CommentDTO dto = CommentDTO.fromEntity(comment, 7L);

        assertThat(dto.getNickname()).isNull();
        assertThat(dto.getAvatar()).isEqualTo("/avatars/u7.png");
        assertThat(dto.isOwner()).isTrue();
    }

    private static User user(String username, String nickname) {
        User user = new User();
        user.setId(7L);
        user.setUsername(username);
        user.setPassword("hashed-password");
        user.setNickname(nickname);
        user.setAvatar("/avatars/u7.png");
        user.setPhone("13800138000");
        user.setIpAddress("203.0.113.99");
        user.setAllowedLoginFingerprintHash("fingerprint-hash");
        return user;
    }
}
