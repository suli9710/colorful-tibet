package com.tibet.tourism.modules.spot.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tibet.tourism.modules.recommendation.application.ColdStartOptimizationService;
import com.tibet.tourism.modules.recommendation.application.ItemBasedRecommendationService;
import com.tibet.tourism.modules.recommendation.application.RecommendationService;
import com.tibet.tourism.modules.spot.application.ScenicSpotService;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.web.dto.ScenicSpotResponse;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ScenicSpotControllerPrivacyTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Mock private ColdStartOptimizationService coldStartOptimizationService;
    @Mock private ItemBasedRecommendationService itemBasedRecommendationService;
    @Mock private RecommendationService recommendationService;
    @Mock private ScenicSpotService scenicSpotService;
    @Mock private UserRepository userRepository;

    private ScenicSpotController controller;

    @BeforeEach
    void setUp() {
        controller = new ScenicSpotController();
        ReflectionTestUtils.setField(controller, "coldStartOptimizationService", coldStartOptimizationService);
        ReflectionTestUtils.setField(controller, "itemBasedRecommendationService", itemBasedRecommendationService);
        ReflectionTestUtils.setField(controller, "recommendationService", recommendationService);
        ReflectionTestUtils.setField(controller, "scenicSpotService", scenicSpotService);
        ReflectionTestUtils.setField(controller, "userRepository", userRepository);
    }

    @Test
    void publicSpotListSerializesWhitelistedDtoFieldsOnly() throws Exception {
        when(scenicSpotService.getAllSpots(any()))
                .thenReturn(new PageImpl<>(List.of(scenicSpot()), PageRequest.of(0, 20), 1));

        var response = controller.getAllSpots(null, "zh", PageRequest.of(0, 20));

        assertThat(response.content()).hasOnlyElementsOfType(ScenicSpotResponse.class);
        String json = OBJECT_MAPPER.writeValueAsString(response);
        assertThat(json)
                .contains("\"name\":\"Potala Palace\"")
                .doesNotContain("\"createdAt\"", "\"num\"");
    }

    @Test
    void publicSpotDetailSerializesWhitelistedLocalizedDtoOnly() throws Exception {
        when(scenicSpotService.getSpotById(11L)).thenReturn(scenicSpot());

        ScenicSpotResponse response = controller.getSpotById(11L, "bo", null);

        assertEquals("bo-name", response.name());
        String json = OBJECT_MAPPER.writeValueAsString(response);
        assertThat(json)
                .contains("\"id\":11")
                .doesNotContain("\"createdAt\"", "\"num\"");
        verify(recommendationService, never()).recordSpotView(any(), any());
    }

    @Test
    void authenticatedSpotDetailRecordsRecommendationBehavior() {
        User user = user(7L, "traveler", User.Role.USER);
        ScenicSpot spot = scenicSpot();
        when(scenicSpotService.getSpotById(11L)).thenReturn(spot);
        when(userRepository.findByUsername("traveler")).thenReturn(Optional.of(user));

        controller.getSpotById(11L, "zh", authentication("traveler"));

        verify(recommendationService).recordSpotView(user, spot);
    }

    @Test
    void publicSimilarSpotsHideRawSimilarityScores() throws Exception {
        Map<Long, Double> similarScores = new LinkedHashMap<>();
        similarScores.put(12L, 0.91);
        similarScores.put(13L, 0.72);
        when(itemBasedRecommendationService.getSimilarSpots(11L, 2)).thenReturn(similarScores);
        when(scenicSpotService.getSpotsByIdsPreservingOrder(List.of(12L, 13L)))
                .thenReturn(List.of(scenicSpot(12L, "Namtso"), scenicSpot(13L, "Yamdrok")));

        var response = controller.getSimilarSpots(11L, 2, "zh");

        assertThat(response)
                .containsEntry("spotId", 11L)
                .containsEntry("count", 2)
                .containsKey("spots")
                .doesNotContainKey("similarSpots");
        String json = OBJECT_MAPPER.writeValueAsString(response);
        assertThat(json)
                .contains("\"name\":\"Namtso\"", "\"name\":\"Yamdrok\"")
                .doesNotContain("0.91", "0.72", "similarity", "score");
    }

    @Test
    void checkIfNewUserDoesNotEchoInternalUserId() {
        User user = user(7L, "traveler", User.Role.USER);
        when(userRepository.findByUsername("traveler")).thenReturn(Optional.of(user));
        when(coldStartOptimizationService.isNewUser(7L)).thenReturn(true);

        var response = controller.checkIfNewUser(
                7L,
                authentication("traveler"));

        assertEquals(true, response.get("isNewUser"));
        assertFalse(response.containsKey("userId"));
    }

    @Test
    void checkIfNewUserRejectsOtherUsersForNonAdmin() {
        User user = user(7L, "traveler", User.Role.USER);
        when(userRepository.findByUsername("traveler")).thenReturn(Optional.of(user));

        assertThrows(
                AccessDeniedException.class,
                () -> controller.checkIfNewUser(
                        8L,
                        authentication("traveler")));
    }

    private static UsernamePasswordAuthenticationToken authentication(String username) {
        return new UsernamePasswordAuthenticationToken(
                username,
                "n/a",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private static User user(Long id, String username, User.Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        return user;
    }

    private static ScenicSpot scenicSpot() {
        return scenicSpot(11L, "Potala Palace");
    }

    private static ScenicSpot scenicSpot(Long id, String name) {
        ScenicSpot spot = new ScenicSpot();
        spot.setId(id);
        spot.setName(name);
        spot.setNameTibetan("bo-name");
        spot.setDescription("Public description");
        spot.setDescriptionTibetan("bo-description");
        spot.setImageUrl("/spots/potala.jpg");
        spot.setAltitude("3700m");
        spot.setLocation("Lhasa");
        spot.setCategory(ScenicSpot.Category.CULTURAL);
        spot.setTicketPrice(BigDecimal.valueOf(200));
        spot.setPeakSeasonPrice(BigDecimal.valueOf(300));
        spot.setOffSeasonPrice(BigDecimal.valueOf(100));
        spot.setRating(BigDecimal.valueOf(4.8));
        spot.setLatitude(BigDecimal.valueOf(29.6578));
        spot.setLongitude(BigDecimal.valueOf(91.1169));
        spot.setVisitCount(12345);
        spot.setNum(30);
        spot.setCreatedAt(LocalDateTime.parse("2026-01-02T03:04:05"));
        return spot;
    }
}
