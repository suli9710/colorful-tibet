package com.tibet.tourism.modules.community.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tibet.tourism.modules.community.domain.Favorite;
import com.tibet.tourism.modules.community.domain.TravelRoute;
import com.tibet.tourism.modules.community.infra.FavoriteRepository;
import com.tibet.tourism.modules.community.infra.TravelRouteRepository;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class FavoriteControllerPrivacyTest {

    @Mock
    private FavoriteRepository favoriteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TravelRouteRepository travelRouteRepository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private FavoriteController controller;

    @BeforeEach
    void setUp() {
        controller = new FavoriteController();
        ReflectionTestUtils.setField(controller, "favoriteRepository", favoriteRepository);
        ReflectionTestUtils.setField(controller, "userRepository", userRepository);
        ReflectionTestUtils.setField(controller, "travelRouteRepository", travelRouteRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void myFavoritesReturnsDtoWithoutUserEntityGraph() throws Exception {
        User user = user("traveler@example.com");
        TravelRoute route = route();
        Favorite favorite = new Favorite();
        favorite.setId(99L);
        favorite.setUser(user);
        favorite.setRoute(route);
        favorite.setCreatedAt(LocalDateTime.parse("2026-06-08T09:30:00"));
        PageRequest pageRequest = PageRequest.of(0, 20);

        authenticate("traveler@example.com");
        when(userRepository.findByUsername("traveler@example.com")).thenReturn(Optional.of(user));
        when(favoriteRepository.findByUserOrderByCreatedAtDesc(eq(user), any()))
                .thenReturn(new PageImpl<>(List.of(favorite), pageRequest, 1));

        ResponseEntity<?> response = controller.getMyFavorites(pageRequest);

        String json = objectMapper.writeValueAsString(response.getBody());
        assertThat(json).contains("\"id\":99");
        assertThat(json).contains("\"route\"");
        assertThat(json).contains("\"name\":\"Lhasa classic\"");
        assertThat(json).doesNotContain("\"user\"");
        assertThat(json).doesNotContain("traveler@example.com");
        assertThat(json).doesNotContain("\"username\"");
        assertThat(json).doesNotContain("\"role\"");
        assertThat(json).doesNotContain("\"phone\"");
        assertThat(json).doesNotContain("\"ipAddress\"");
        assertThat(json).doesNotContain("\"sessionVersion\"");
        assertThat(json).doesNotContain("\"password\"");
        assertThat(json).doesNotContain("fingerprint-secret");
    }

    @Test
    void addFavoriteTreatsConcurrentDuplicateAsIdempotentSuccess() {
        User user = user("traveler@example.com");
        TravelRoute route = route();

        authenticate("traveler@example.com");
        when(userRepository.findByUsername("traveler@example.com")).thenReturn(Optional.of(user));
        when(travelRouteRepository.findById(123L)).thenReturn(Optional.of(route));
        when(favoriteRepository.existsByUserAndRoute(user, route)).thenReturn(false);
        doThrow(new DataIntegrityViolationException("duplicate favorite"))
                .when(favoriteRepository)
                .saveAndFlush(any(Favorite.class));
        when(favoriteRepository.countByRoute(route)).thenReturn(1L);

        ResponseEntity<?> response = controller.addFavorite(123L);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isInstanceOf(Map.class);
        Map<?, ?> body = (Map<?, ?>) response.getBody();
        assertThat(body.get("favorited")).isEqualTo(true);
        assertThat(body.get("favoriteCount")).isEqualTo(1L);
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

    private User user(String username) {
        User user = new User();
        user.setId(7L);
        user.setUsername(username);
        user.setPassword("hashed-password");
        user.setNickname("Traveler");
        user.setAvatar("/uploads/avatars/private.jpg");
        user.setPhone("+15551234567");
        user.setRole(User.Role.ADMIN);
        user.setCity("Lhasa");
        user.setIpAddress("ip-hash-secret");
        user.setAllowedLoginFingerprintHash("fingerprint-secret");
        user.setSessionVersion(12L);
        return user;
    }

    private TravelRoute route() {
        TravelRoute route = new TravelRoute();
        route.setId(123L);
        route.setName("Lhasa classic");
        route.setNameTibetan("lha sa");
        route.setDescription("Potala and Jokhang");
        route.setDescriptionTibetan("description");
        route.setDays(5);
        route.setPrice(BigDecimal.valueOf(1999));
        route.setDifficulty(TravelRoute.Difficulty.EASY);
        route.setTemperature("10C - 20C");
        route.setGeography("plateau");
        return route;
    }
}
