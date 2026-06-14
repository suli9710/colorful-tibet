package com.tibet.tourism.modules.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.api.PageResponse;
import com.tibet.tourism.modules.community.domain.Comment;
import com.tibet.tourism.modules.community.domain.RouteComment;
import com.tibet.tourism.modules.community.domain.SharedRoute;
import com.tibet.tourism.modules.community.infra.CommentRepository;
import com.tibet.tourism.modules.community.infra.RouteCommentRepository;
import com.tibet.tourism.modules.community.infra.SharedRouteRepository;
import com.tibet.tourism.modules.community.web.dto.CommentDTO;
import com.tibet.tourism.modules.community.web.dto.RouteCommentResponse;
import com.tibet.tourism.modules.order.infra.BookingRepository;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
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
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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
        ScenicSpot spot = new ScenicSpot();
        spot.setId(30L);
        spot.setName("Potala Palace");
        spotComment.setSpot(spot);

        RouteComment routeComment = new RouteComment();
        routeComment.setId(20L);
        routeComment.setUser(user);
        routeComment.setContent("Route comment");
        routeComment.setCreatedAt(LocalDateTime.parse("2026-01-02T04:04:05"));
        SharedRoute route = new SharedRoute();
        route.setId(40L);
        route.setTitle("Lhasa classic route");
        routeComment.setRoute(route);

        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(commentRepository.findByUser(eq(user), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(spotComment), PageRequest.of(0, 20), 1));
        when(routeCommentRepository.findByUser(eq(user), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(routeComment), PageRequest.of(0, 20), 1));

        Map<String, Object> comments = service.getComments(7L);

        assertThat((List<?>) comments.get("spotComments")).hasOnlyElementsOfType(CommentDTO.class);
        assertThat((List<?>) comments.get("routeComments")).hasOnlyElementsOfType(RouteCommentResponse.class);
        assertThat(comments.get("spotCommentsPage")).isInstanceOf(PageResponse.class);
        assertThat(comments.get("routeCommentsPage")).isInstanceOf(PageResponse.class);
        assertThat(comments.get("spotComments").toString()).doesNotContain("hashed-password", "13800138000", "203.0.113.99");
        assertThat(comments.get("routeComments").toString()).doesNotContain("hashed-password", "13800138000", "203.0.113.99");
        CommentDTO spotDto = (CommentDTO) ((List<?>) comments.get("spotComments")).get(0);
        RouteCommentResponse routeDto = (RouteCommentResponse) ((List<?>) comments.get("routeComments")).get(0);
        assertThat(spotDto.getSpot())
                .extracting(CommentDTO.ParentSpotResponse::id, CommentDTO.ParentSpotResponse::name)
                .containsExactly(30L, "Potala Palace");
        assertThat(routeDto.route())
                .extracting(RouteCommentResponse.ParentRouteResponse::id, RouteCommentResponse.ParentRouteResponse::title)
                .containsExactly(40L, "Lhasa classic route");
    }

    @Test
    void getCommentsClampsRequestedPageSize() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(commentRepository.findByUser(eq(user), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 50), 0));
        when(routeCommentRepository.findByUser(eq(user), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(2, 50), 0));

        service.getComments(7L, PageRequest.of(2, 500));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(commentRepository).findByUser(eq(user), pageableCaptor.capture());
        assertThat(pageableCaptor.getValue().getPageNumber()).isEqualTo(2);
        assertThat(pageableCaptor.getValue().getPageSize()).isEqualTo(50);
    }

    @Test
    void getStatsUsesCountQueriesInsteadOfLoadingHistories() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(sharedRouteRepository.count(ArgumentMatchers.<Specification<SharedRoute>>any()))
                .thenReturn(3L);
        when(routeCommentRepository.countByUser(user)).thenReturn(2L);
        when(commentRepository.countByUser(user)).thenReturn(4L);
        when(bookingRepository.countByUserId(7L)).thenReturn(5L);

        Map<String, Object> stats = service.getStats(7L);

        assertThat(stats)
                .containsEntry("routeCount", 3L)
                .containsEntry("commentCount", 6L)
                .containsEntry("bookingCount", 5L);
        verify(sharedRouteRepository, never()).findByAuthor(any(User.class), any(Pageable.class));
        verify(bookingRepository, never()).findByUserId(7L);
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

    @Test
    void updateNicknameRejectsNicknameMatchingAnotherUsername() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.existsByUsernameIgnoreCase("admin")).thenReturn(true);

        assertThatThrownBy(() -> service.updateNickname(7L, "admin"))
                .isInstanceOf(DataIntegrityViolationException.class);

        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void updateNicknameAllowsNicknameMatchingOwnUsername() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(userRepository.existsByNickname("login-name")).thenReturn(false);

        String nickname = service.updateNickname(7L, "login-name");

        assertThat(nickname).isEqualTo("login-name");
        assertThat(user.getNickname()).isEqualTo("login-name");
        verify(userRepository).saveAndFlush(user);
        verify(userRepository, never()).existsByUsernameIgnoreCase(any());
    }
}
