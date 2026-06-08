package com.tibet.tourism.modules.community.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.modules.community.application.SharedRouteService;
import com.tibet.tourism.modules.community.application.TravelQAService;
import com.tibet.tourism.modules.community.domain.Favorite;
import com.tibet.tourism.modules.community.domain.SharedRoute;
import com.tibet.tourism.modules.community.domain.TravelQuestion;
import com.tibet.tourism.modules.community.domain.TravelRoute;
import com.tibet.tourism.modules.community.infra.FavoriteRepository;
import com.tibet.tourism.modules.community.infra.TravelRouteRepository;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CommunityPageResponseContractTest {

    @Mock
    private SharedRouteService sharedRouteService;

    @Mock
    private TravelQAService travelQAService;

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TravelRouteRepository travelRouteRepository;

    @Mock
    private JwtAuthSupport jwtAuthSupport;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sharedRoutesReturnStablePageEnvelopeWithoutSpringDataInternals() throws Exception {
        SharedRouteController controller = new SharedRouteController();
        ReflectionTestUtils.setField(controller, "routeService", sharedRouteService);
        ReflectionTestUtils.setField(controller, "userRepository", userRepository);
        ReflectionTestUtils.setField(controller, "jwtAuthSupport", jwtAuthSupport);

        PageRequest pageRequest = PageRequest.of(1, 2);
        when(jwtAuthSupport.resolveOptionalCurrentUser(any())).thenReturn(Optional.empty());
        when(sharedRouteService.getRoutes(isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(sharedRoute()), pageRequest, 5));

        ResponseEntity<?> response = controller.getSharedRoutes(
                null, null, null, 1, 2, "createdAt", new MockHttpServletRequest());

        assertStablePageEnvelope(response.getBody(), 10L);
    }

    @Test
    void questionsReturnStablePageEnvelopeWithoutSpringDataInternals() throws Exception {
        TravelQAController controller = new TravelQAController();
        ReflectionTestUtils.setField(controller, "qaService", travelQAService);
        ReflectionTestUtils.setField(controller, "jwtAuthSupport", jwtAuthSupport);

        PageRequest pageRequest = PageRequest.of(1, 2);
        when(jwtAuthSupport.resolveOptionalCurrentUser(any())).thenReturn(Optional.empty());
        when(travelQAService.getQuestions(isNull(), eq("latest"), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(question()), pageRequest, 5));

        ResponseEntity<?> response = controller.getQuestions(
                null, "latest", null, 1, 2, new MockHttpServletRequest());

        assertStablePageEnvelope(response.getBody(), 20L);
    }

    @Test
    void myFavoritesReturnStablePageEnvelopeWithoutSpringDataInternals() throws Exception {
        FavoriteController controller = new FavoriteController();
        ReflectionTestUtils.setField(controller, "favoriteRepository", favoriteRepository);
        ReflectionTestUtils.setField(controller, "userRepository", userRepository);
        ReflectionTestUtils.setField(controller, "travelRouteRepository", travelRouteRepository);

        User user = publicUser();
        PageRequest pageRequest = PageRequest.of(1, 2);
        authenticate(user.getUsername());
        when(userRepository.findByUsername(user.getUsername())).thenReturn(Optional.of(user));
        when(favoriteRepository.findByUserOrderByCreatedAtDesc(eq(user), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(favorite(user)), pageRequest, 5));

        ResponseEntity<?> response = controller.getMyFavorites(pageRequest);

        assertStablePageEnvelope(response.getBody(), 30L);
    }

    private void assertStablePageEnvelope(Object body, long expectedFirstContentId) throws Exception {
        String json = objectMapper.writeValueAsString(body);
        JsonNode root = objectMapper.readTree(json);

        assertThat(root.get("content").isArray()).isTrue();
        assertThat(root.get("content").get(0).get("id").asLong()).isEqualTo(expectedFirstContentId);
        assertThat(root.get("page").asInt()).isEqualTo(1);
        assertThat(root.get("size").asInt()).isEqualTo(2);
        assertThat(root.get("totalElements").asLong()).isEqualTo(5);
        assertThat(root.get("totalPages").asInt()).isEqualTo(3);

        assertThat(root.has("pageable")).isFalse();
        assertThat(root.has("sort")).isFalse();
        assertThat(root.has("number")).isFalse();
        assertThat(root.has("numberOfElements")).isFalse();
        assertThat(root.has("first")).isFalse();
        assertThat(root.has("last")).isFalse();
        assertThat(root.has("empty")).isFalse();

        assertThat(json)
                .doesNotContain("\"pageable\"")
                .doesNotContain("\"sort\"")
                .doesNotContain("\"number\"")
                .doesNotContain("\"numberOfElements\"")
                .doesNotContain("\"first\"")
                .doesNotContain("\"last\"")
                .doesNotContain("\"empty\"");
    }

    private void authenticate(String username) {
        UserDetails principal = org.springframework.security.core.userdetails.User
                .withUsername(username)
                .password("ignored")
                .authorities("ROLE_USER")
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    private SharedRoute sharedRoute() {
        SharedRoute route = new SharedRoute();
        route.setId(10L);
        route.setAuthor(publicUser());
        route.setTitle("Everest loop");
        route.setContent("Shared route content");
        route.setDays(6);
        route.setBudget("comfort");
        route.setPreference("natural");
        route.setSourceType(SharedRoute.SourceType.USER);
        route.setPrice(new BigDecimal("2999.00"));
        route.setDifficulty("medium");
        route.setTemperature("5C - 15C");
        route.setGeography("plateau");
        route.setViewCount(8);
        route.setLikeCount(3);
        route.setCommentCount(1);
        route.setCreatedAt(LocalDateTime.parse("2026-06-01T10:00:00"));
        route.setUpdatedAt(LocalDateTime.parse("2026-06-01T10:30:00"));
        return route;
    }

    private TravelQuestion question() {
        TravelQuestion question = new TravelQuestion();
        question.setId(20L);
        question.setAuthor(publicUser());
        question.setTitle("How to plan acclimation?");
        question.setContent("Question content");
        question.setTags("health,route");
        question.setViewCount(7);
        question.setAnswerCount(2);
        question.setLikeCount(4);
        question.setIsResolved(false);
        question.setCreatedAt(LocalDateTime.parse("2026-06-02T11:00:00"));
        question.setUpdatedAt(LocalDateTime.parse("2026-06-02T11:30:00"));
        return question;
    }

    private Favorite favorite(User user) {
        Favorite favorite = new Favorite();
        favorite.setId(30L);
        favorite.setUser(user);
        favorite.setRoute(travelRoute());
        favorite.setCreatedAt(LocalDateTime.parse("2026-06-03T12:00:00"));
        return favorite;
    }

    private TravelRoute travelRoute() {
        TravelRoute route = new TravelRoute();
        route.setId(40L);
        route.setName("Lhasa classic");
        route.setNameTibetan("lha sa");
        route.setDescription("Potala and Jokhang");
        route.setDescriptionTibetan("description");
        route.setDays(5);
        route.setPrice(new BigDecimal("1999.00"));
        route.setDifficulty(TravelRoute.Difficulty.EASY);
        route.setTemperature("10C - 20C");
        route.setGeography("plateau");
        return route;
    }

    private User publicUser() {
        User user = new User();
        user.setId(7L);
        user.setUsername("traveler@example.com");
        user.setNickname("Traveler");
        user.setAvatar("/avatars/u7.png");
        return user;
    }
}
