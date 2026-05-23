package com.tibet.tourism.modules.community.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.modules.community.domain.SharedRoute;
import com.tibet.tourism.modules.community.infra.CommentRepository;
import com.tibet.tourism.modules.community.infra.RouteCommentRepository;
import com.tibet.tourism.modules.community.infra.RouteLikeRepository;
import com.tibet.tourism.modules.community.infra.SharedRouteRepository;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

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
        when(routeRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

        service.getRoutes(null, "economy", "relaxation", PageRequest.of(0, 10));
        service.getRoutes(null, "经济型", "休闲度假", PageRequest.of(0, 10));

        verify(routeRepository, times(2)).findAll(any(Specification.class), any(Pageable.class));
    }

    private User user() {
        User user = new User();
        user.setId(7L);
        user.setUsername("traveler");
        return user;
    }
}
