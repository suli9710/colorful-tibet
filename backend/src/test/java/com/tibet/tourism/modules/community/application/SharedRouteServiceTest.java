package com.tibet.tourism.modules.community.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.modules.community.domain.RouteComment;
import com.tibet.tourism.modules.community.domain.SharedRoute;
import com.tibet.tourism.modules.community.infra.CommentRepository;
import com.tibet.tourism.modules.community.infra.RouteCommentRepository;
import com.tibet.tourism.modules.community.infra.RouteLikeRepository;
import com.tibet.tourism.modules.community.infra.SharedRouteRepository;
import com.tibet.tourism.modules.community.infra.SharedRouteSummaryRow;
import com.tibet.tourism.modules.community.web.dto.SharedRouteSummaryResponse;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class SharedRouteServiceTest {

    @Mock
    private SharedRouteRepository routeRepository;
    @Mock
    private RouteCommentRepository commentRepository;
    @Mock
    private RouteLikeRepository likeRepository;
    @Mock
    private CommentRepository legacyCommentRepository;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SharedRouteService service;

    @Test
    void shareRouteNormalizesCanonicalBudgetAndPreferenceKeys() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));
        when(routeRepository.save(any(SharedRoute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SharedRoute route = service.shareRoute(7L, "Route title", "Route content", 5, "comfort", "natural");

        assertThat(route.getBudget()).isEqualTo("舒适型");
        assertThat(route.getPreference()).isEqualTo("自然风光");
    }

    @Test
    void shareRouteStillAcceptsChineseBudgetAndPreferenceValues() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));
        when(routeRepository.save(any(SharedRoute.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SharedRoute route = service.shareRoute(7L, "Route title", "Route content", 5, "豪华型", "深度摄影");

        assertThat(route.getBudget()).isEqualTo("豪华型");
        assertThat(route.getPreference()).isEqualTo("深度摄影");
    }

    @Test
    void shareRouteRejectsInvalidBudgetAndPreferenceValues() {
        when(userRepository.findById(7L)).thenReturn(Optional.of(user()));

        assertThatThrownBy(() -> service.shareRoute(7L, "Route title", "Route content", 5, "premium", "natural"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("预算不合法");
        assertThatThrownBy(() -> service.shareRoute(7L, "Route title", "Route content", 5, "comfort", "wildlife"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("旅行偏好不合法");
        verify(routeRepository, never()).save(any());
    }

    @Test
    void routeFiltersAcceptCanonicalKeysAndChineseValues() {
        when(routeRepository.findSummaries(any(), any(), any(), any(Pageable.class))).thenReturn(Page.empty());

        service.getRoutes(null, "economy", "relaxation", PageRequest.of(0, 10));
        service.getRoutes(null, "经济型", "休闲度假", PageRequest.of(0, 10));

        verify(routeRepository, times(2))
                .findSummaries(isNull(), eq("经济型"), eq("休闲度假"), any(Pageable.class));
    }

    @Test
    void getRoutesMapsSummaryProjectionWithoutRouteEntityContent() {
        Pageable pageable = PageRequest.of(0, 10);
        when(routeRepository.findSummaries(null, null, null, pageable))
                .thenReturn(new PageImpl<>(List.of(summaryRow(100L, 7L)), pageable, 1));

        Page<SharedRouteSummaryResponse> result = service.getRoutes(null, null, null, pageable, 7L);

        SharedRouteSummaryResponse summary = result.getContent().get(0);
        assertThat(summary.id()).isEqualTo(100L);
        assertThat(summary.title()).isEqualTo("Lhasa route");
        assertThat(summary.author().owner()).isTrue();
        assertThat(summary.author().nickname()).isEqualTo("Tibet Traveler");
        verify(routeRepository).findSummaries(null, null, null, pageable);
    }

    @Test
    void getCommentsUsesPagedRepositoryLookup() {
        SharedRoute route = new SharedRoute();
        route.setId(99L);
        RouteComment comment = new RouteComment();
        comment.setId(300L);
        Pageable pageable = PageRequest.of(2, 20);
        when(routeRepository.findById(99L)).thenReturn(Optional.of(route));
        when(commentRepository.findByRoute(route, pageable))
                .thenReturn(new PageImpl<>(List.of(comment), pageable, 41));

        Page<RouteComment> result = service.getComments(99L, pageable);

        assertThat(result.getContent()).containsExactly(comment);
        assertThat(result.getTotalElements()).isEqualTo(41);
        verify(commentRepository).findByRoute(route, pageable);
    }

    @Test
    void getRoutesByAuthorUsesPagedRepositoryLookup() {
        User author = user();
        Pageable pageable = PageRequest.of(0, 1);
        when(routeRepository.findSummariesByAuthorId(author.getId(), pageable))
                .thenReturn(new PageImpl<>(List.of(summaryRow(100L, author.getId())), pageable, 6));

        Page<SharedRouteSummaryResponse> result = service.getRoutesByAuthor(author, pageable);

        assertThat(result.getContent().get(0).id()).isEqualTo(100L);
        assertThat(result.getContent().get(0).author().owner()).isTrue();
        assertThat(result.getTotalElements()).isEqualTo(6);
        verify(routeRepository).findSummariesByAuthorId(author.getId(), pageable);
    }

    @Test
    void likeRouteReturnsUpdatedCountWithoutIncrementingViews() {
        User user = user();
        SharedRoute route = sharedRoute(user);
        when(routeRepository.findById(100L)).thenReturn(Optional.of(route));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(likeRepository.insertIgnore(100L, 7L)).thenReturn(1);
        when(routeRepository.findLikeCountById(100L)).thenReturn(Optional.of(3));

        SharedRouteService.LikeResult result = service.likeRoute(100L, 7L);

        assertThat(result.liked()).isTrue();
        assertThat(result.likeCount()).isEqualTo(3);
        verify(routeRepository).incrementLikeCount(100L);
        verify(routeRepository, never()).incrementViewCount(100L);
    }

    @Test
    void duplicateRouteLikeIsIdempotentAndDoesNotIncrementAgain() {
        User user = user();
        SharedRoute route = sharedRoute(user);
        when(routeRepository.findById(100L)).thenReturn(Optional.of(route));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(likeRepository.insertIgnore(100L, 7L)).thenReturn(0);
        when(routeRepository.findLikeCountById(100L)).thenReturn(Optional.of(1));

        SharedRouteService.LikeResult result = service.likeRoute(100L, 7L);

        assertThat(result.liked()).isTrue();
        assertThat(result.likeCount()).isEqualTo(1);
        verify(routeRepository, never()).incrementLikeCount(100L);
        verify(routeRepository, never()).incrementViewCount(100L);
    }

    @Test
    void deleteRouteCleansChildRowsBeforeDeletingOwnedRoute() {
        User author = user();
        SharedRoute route = sharedRoute(author);
        route.setCommentCount(2);
        route.setLikeCount(1);
        when(routeRepository.findById(100L)).thenReturn(Optional.of(route));

        service.deleteRoute(100L, 7L);

        InOrder orderedDeletes = inOrder(likeRepository, commentRepository, routeRepository);
        orderedDeletes.verify(routeRepository).findById(100L);
        orderedDeletes.verify(likeRepository).deleteByRouteId(100L);
        orderedDeletes.verify(commentRepository).deleteByRouteId(100L);
        orderedDeletes.verify(routeRepository).delete(route);
    }

    private SharedRouteSummaryRow summaryRow(Long routeId, Long authorId) {
        return new SharedRouteSummaryRow(
                routeId,
                authorId,
                "traveler",
                "Tibet Traveler",
                "/avatars/u7.png",
                "Lhasa route",
                3,
                "舒适型",
                "自然风光",
                SharedRoute.SourceType.USER,
                null,
                null,
                "medium",
                "5C - 15C",
                "plateau",
                12,
                2,
                0,
                LocalDateTime.parse("2026-01-02T03:04:05"),
                LocalDateTime.parse("2026-01-03T03:04:05"));
    }

    private User user() {
        User user = new User();
        user.setId(7L);
        user.setUsername("traveler");
        return user;
    }

    private SharedRoute sharedRoute(User author) {
        SharedRoute route = new SharedRoute();
        route.setId(100L);
        route.setAuthor(author);
        route.setTitle("Lhasa route");
        route.setContent("Route content");
        route.setDays(3);
        route.setViewCount(12);
        route.setLikeCount(2);
        route.setCommentCount(0);
        return route;
    }
}
