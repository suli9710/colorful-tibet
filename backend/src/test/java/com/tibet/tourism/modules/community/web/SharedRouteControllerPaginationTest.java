package com.tibet.tourism.modules.community.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.modules.community.application.SharedRouteService;
import com.tibet.tourism.modules.community.domain.RouteComment;
import com.tibet.tourism.modules.community.domain.SharedRoute;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SharedRouteControllerPaginationTest {

    @Mock
    private SharedRouteService routeService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private JwtAuthSupport jwtAuthSupport;
    @Mock
    private HttpServletRequest request;

    private SharedRouteController controller;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach
    void setUp() {
        controller = new SharedRouteController();
        ReflectionTestUtils.setField(controller, "routeService", routeService);
        ReflectionTestUtils.setField(controller, "userRepository", userRepository);
        ReflectionTestUtils.setField(controller, "jwtAuthSupport", jwtAuthSupport);
    }

    @Test
    void sharedRouteCommentsClampPageSizeAndUseStableSortWithHeaders() {
        RouteComment comment = routeComment();
        when(routeService.getComments(eq(100L), any(Pageable.class))).thenAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(1, Pageable.class);
            return new PageImpl<>(List.of(comment), pageable, 72);
        });

        ResponseEntity<?> response = controller.getComments(100L, -4, 500, request);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(routeService).getComments(eq(100L), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(50);
        assertDescendingSort(pageable, "createdAt");
        assertDescendingSort(pageable, "id");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("X-Page")).isEqualTo("0");
        assertThat(response.getHeaders().getFirst("X-Size")).isEqualTo("50");
        assertThat(response.getHeaders().getFirst("X-Total-Elements")).isEqualTo("72");
        assertThat(response.getHeaders().getFirst("X-Total-Pages")).isEqualTo("2");
        assertThat(response.getBody()).isInstanceOf(List.class);
    }

    @Test
    void myRoutesUseDefaultPageSizeAndReturnSummaryWithoutFullContent() throws Exception {
        User user = user();
        SharedRoute route = sharedRoute(user);
        when(jwtAuthSupport.resolveCurrentUserId(request)).thenReturn(user.getId());
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(routeService.getRoutesByAuthor(eq(user), any(Pageable.class))).thenAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(1, Pageable.class);
            return new PageImpl<>(List.of(route), pageable, 1);
        });

        ResponseEntity<?> response = controller.getMyRoutes(request, -1, 0);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(routeService).getRoutesByAuthor(eq(user), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();
        assertThat(pageable.getPageNumber()).isZero();
        assertThat(pageable.getPageSize()).isEqualTo(20);
        assertDescendingSort(pageable, "createdAt");
        assertDescendingSort(pageable, "id");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("X-Page")).isEqualTo("0");
        assertThat(response.getHeaders().getFirst("X-Size")).isEqualTo("20");
        assertThat(response.getHeaders().getFirst("X-Total-Elements")).isEqualTo("1");
        assertThat(response.getHeaders().getFirst("X-Total-Pages")).isEqualTo("1");

        String json = objectMapper.writeValueAsString(response.getBody());
        JsonNode root = objectMapper.readTree(json);
        assertThat(root.isArray()).isTrue();
        assertThat(root.get(0).get("id").asLong()).isEqualTo(100L);
        assertThat(root.get(0).has("content")).isFalse();
        assertThat(json).doesNotContain("full route content that belongs on detail only");
    }

    @Test
    void sharedRoutesReturnSummaryWithoutFullContent() throws Exception {
        User user = user();
        SharedRoute route = sharedRoute(user);
        when(jwtAuthSupport.resolveOptionalCurrentUser(request)).thenReturn(Optional.empty());
        when(routeService.getRoutes(isNull(), isNull(), isNull(), any(Pageable.class))).thenAnswer(invocation -> {
            Pageable pageable = invocation.getArgument(3, Pageable.class);
            return new PageImpl<>(List.of(route), pageable, 1);
        });

        ResponseEntity<?> response = controller.getSharedRoutes(null, null, null, 0, 20, "createdAt", request);

        String json = objectMapper.writeValueAsString(response.getBody());
        JsonNode root = objectMapper.readTree(json);
        assertThat(root.get("content").isArray()).isTrue();
        assertThat(root.get("content").get(0).get("id").asLong()).isEqualTo(100L);
        assertThat(root.get("content").get(0).has("content")).isFalse();
        assertThat(json).doesNotContain("full route content that belongs on detail only");
    }

    @Test
    void likeRouteReturnsCountWithoutLoadingDetail() {
        when(jwtAuthSupport.resolveCurrentUserId(request)).thenReturn(7L);
        when(routeService.likeRoute(100L, 7L))
                .thenReturn(new SharedRouteService.LikeResult(true, 3));

        ResponseEntity<?> response = controller.likeRoute(100L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body.get("liked")).isEqualTo(true);
        assertThat(body.get("likeCount")).isEqualTo(3);
        verify(routeService, never()).getRoute(any());
    }

    @Test
    void unlikeRouteReturnsCountWithoutLoadingDetail() {
        when(jwtAuthSupport.resolveCurrentUserId(request)).thenReturn(7L);
        when(routeService.unlikeRoute(100L, 7L))
                .thenReturn(new SharedRouteService.LikeResult(false, 2));

        ResponseEntity<?> response = controller.unlikeRoute(100L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body.get("liked")).isEqualTo(false);
        assertThat(body.get("likeCount")).isEqualTo(2);
        verify(routeService, never()).getRoute(any());
    }

    private void assertDescendingSort(Pageable pageable, String field) {
        Sort.Order order = pageable.getSort().getOrderFor(field);
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(Sort.Direction.DESC);
    }

    private static RouteComment routeComment() {
        RouteComment comment = new RouteComment();
        comment.setId(300L);
        comment.setContent("Nice route");
        comment.setCreatedAt(LocalDateTime.parse("2026-01-02T03:04:05"));
        return comment;
    }

    private static SharedRoute sharedRoute(User author) {
        SharedRoute route = new SharedRoute();
        route.setId(100L);
        route.setAuthor(author);
        route.setTitle("Lhasa route");
        route.setContent("full route content that belongs on detail only");
        route.setDays(3);
        route.setBudget("comfort");
        route.setPreference("natural");
        route.setViewCount(10);
        route.setLikeCount(2);
        route.setCommentCount(1);
        route.setCreatedAt(LocalDateTime.parse("2026-01-02T03:04:05"));
        route.setUpdatedAt(LocalDateTime.parse("2026-01-03T03:04:05"));
        return route;
    }

    private static User user() {
        User user = new User();
        user.setId(7L);
        user.setUsername("traveler");
        user.setNickname("Traveler");
        return user;
    }
}
