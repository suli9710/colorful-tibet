package com.tibet.tourism.modules.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.modules.community.domain.Comment;
import com.tibet.tourism.modules.community.domain.RouteComment;
import com.tibet.tourism.modules.community.infra.CommentRepository;
import com.tibet.tourism.modules.community.infra.RouteCommentRepository;
import com.tibet.tourism.modules.community.infra.SharedRouteRepository;
import com.tibet.tourism.modules.community.web.dto.CommentDTO;
import com.tibet.tourism.modules.community.web.dto.RouteCommentResponse;
import com.tibet.tourism.modules.order.infra.BookingRepository;
import com.tibet.tourism.modules.upload.application.FileStorageService;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CurrentUserApplicationServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private SharedRouteRepository sharedRouteRepository;
    @Mock private RouteCommentRepository routeCommentRepository;
    @Mock private BookingRepository bookingRepository;
    @Mock private CommentRepository commentRepository;
    @Mock private UserService userService;
    @Mock private FileStorageService fileStorageService;

    private CurrentUserApplicationService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new CurrentUserApplicationService(
                userRepository,
                sharedRouteRepository,
                routeCommentRepository,
                bookingRepository,
                commentRepository,
                userService,
                fileStorageService);

        user = new User();
        user.setId(7L);
        user.setUsername("login-name");
        user.setPassword("hashed-password");
        user.setNickname("Public Nickname");
        user.setAvatar("/avatars/public.png");
        user.setPhone("13800138000");
        user.setIpAddress("203.0.113.99");
        user.setAllowedLoginFingerprintHash("fingerprint-hash");
        user.setRole(User.Role.ADMIN);
        user.setMustChangePassword(true);
    }

    @Test
    void getProfileReturnsSessionProfileWithoutInternalIdentifiers() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        Map<String, Object> profile = service.getProfile(7L);

        assertThat(profile)
                .doesNotContainKeys("id", "username")
                .containsEntry("nickname", "Public Nickname")
                .containsEntry("avatar", "/avatars/public.png")
                .containsEntry("avatarUrl", "/avatars/public.png")
                .containsEntry("role", User.Role.ADMIN)
                .containsEntry("mustChangePassword", true);
    }

    @Test
    void getProfileSuppressesNicknameWhenItMatchesLoginName() {
        user.setNickname(" login-name ");
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        Map<String, Object> profile = service.getProfile(7L);

        assertThat(profile).doesNotContainKeys("id", "username", "nickname");
    }

    @Test
    void getCommentsReturnsDtosInsteadOfJpaEntities() {
        Comment spotComment = new Comment();
        spotComment.setId(10L);
        spotComment.setUser(user);
        spotComment.setContent("Spot comment");
        spotComment.setRating(5);
        spotComment.setCreatedAt(LocalDateTime.parse("2026-01-02T03:04:05"));

        RouteComment routeComment = new RouteComment();
        routeComment.setId(20L);
        routeComment.setUser(user);
        routeComment.setContent("Route comment");
        routeComment.setCreatedAt(LocalDateTime.parse("2026-01-02T04:04:05"));

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(commentRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(spotComment));
        when(routeCommentRepository.findByUserOrderByCreatedAtDesc(user)).thenReturn(List.of(routeComment));

        Map<String, Object> comments = service.getComments(7L);

        assertThat((List<?>) comments.get("spotComments")).hasOnlyElementsOfType(CommentDTO.class);
        assertThat((List<?>) comments.get("routeComments")).hasOnlyElementsOfType(RouteCommentResponse.class);
        assertThat(comments.get("spotComments").toString()).doesNotContain("hashed-password", "13800138000", "203.0.113.99");
        assertThat(comments.get("routeComments").toString()).doesNotContain("hashed-password", "13800138000", "203.0.113.99");
    }

    @Test
    void updateAvatarStoresOnlyLocalAvatarResourcePath() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        String avatarUrl = service.updateAvatar(7L, "  /uploads/avatars/user.png  ");

        assertThat(avatarUrl).isEqualTo("/uploads/avatars/user.png");
        assertThat(user.getAvatar()).isEqualTo("/uploads/avatars/user.png");
        verify(userRepository).save(user);
    }

    @Test
    void updateAvatarAcceptsLegacyLocalAvatarPath() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));

        String avatarUrl = service.updateAvatar(7L, "/avatars/u7.png");

        assertThat(avatarUrl).isEqualTo("/avatars/u7.png");
        assertThat(user.getAvatar()).isEqualTo("/avatars/u7.png");
        verify(userRepository).save(user);
    }

    @Test
    void updateAvatarRejectsExternalAvatarUrlBeforeLoadingUser() {
        assertThatThrownBy(() -> service.updateAvatar(7L, "https://cdn.example.com/avatar.png?signature=secret"))
                .isInstanceOf(IllegalArgumentException.class);

        verify(userRepository, never()).findById(7L);
        verify(userRepository, never()).save(any(User.class));
    }
}
