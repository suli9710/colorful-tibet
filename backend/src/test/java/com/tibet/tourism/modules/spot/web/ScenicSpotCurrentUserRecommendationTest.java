package com.tibet.tourism.modules.spot.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.tibet.tourism.common.security.AuthEntryPointJwt;
import com.tibet.tourism.common.security.CsrfCookieFilter;
import com.tibet.tourism.common.security.CsrfTokenService;
import com.tibet.tourism.common.security.JwtUtils;
import com.tibet.tourism.common.security.TokenRevocationService;
import com.tibet.tourism.common.security.TrustedProxyIpResolver;
import com.tibet.tourism.common.security.UserDetailsServiceImpl;
import com.tibet.tourism.common.security.UserSessionVersionService;
import com.tibet.tourism.common.security.WebSecurityConfig;
import com.tibet.tourism.modules.recommendation.application.ColdStartOptimizationService;
import com.tibet.tourism.modules.recommendation.application.ItemBasedRecommendationService;
import com.tibet.tourism.modules.recommendation.application.RecommendationService;
import com.tibet.tourism.modules.recommendation.web.dto.RecommendationContext;
import com.tibet.tourism.modules.spot.application.CompanionInferenceService;
import com.tibet.tourism.modules.spot.application.ScenicSpotService;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = ScenicSpotController.class)
@Import({
        WebSecurityConfig.class,
        AuthEntryPointJwt.class,
        CsrfCookieFilter.class,
        TrustedProxyIpResolver.class
})
class ScenicSpotCurrentUserRecommendationTest {

    private static final String USER_TOKEN = "user-token";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private CsrfTokenService csrfTokenService;

    @MockBean
    private TokenRevocationService tokenRevocationService;

    @MockBean
    private UserSessionVersionService userSessionVersionService;

    @MockBean
    private ScenicSpotService scenicSpotService;

    @MockBean
    private RecommendationService recommendationService;

    @MockBean
    private CompanionInferenceService companionInferenceService;

    @MockBean
    private ItemBasedRecommendationService itemBasedRecommendationService;

    @MockBean
    private ColdStartOptimizationService coldStartOptimizationService;

    @MockBean
    private UserRepository userRepository;

    @Test
    void getMyRecommendationsUsesAuthenticatedUserWithoutUserId() throws Exception {
        authenticateAs(user(42L, "traveler", User.Role.USER));
        when(recommendationService.recommendSpotsForUser(eq(42L), nullable(RecommendationContext.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/spots/recommendations/me")
                        .header("Authorization", "Bearer " + USER_TOKEN)
                        .param("season", "WINTER")
                        .param("considerBudget", "false"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        ArgumentCaptor<RecommendationContext> contextCaptor = ArgumentCaptor.forClass(RecommendationContext.class);
        verify(recommendationService).recommendSpotsForUser(eq(42L), contextCaptor.capture());
        assertThat(contextCaptor.getValue().getSeason()).isEqualTo("WINTER");
        assertThat(contextCaptor.getValue().getConsiderBudget()).isFalse();
    }

    @Test
    void getMyRecommendationsIgnoresUserIdQueryParameter() throws Exception {
        authenticateAs(user(42L, "traveler", User.Role.USER));
        when(recommendationService.recommendSpotsForUser(eq(42L), nullable(RecommendationContext.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/spots/recommendations/me")
                        .header("Authorization", "Bearer " + USER_TOKEN)
                        .param("userId", "999"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(recommendationService).recommendSpotsForUser(eq(42L), isNull());
        verifyNoMoreInteractions(recommendationService);
    }

    @Test
    void postMyRecommendationsIgnoresQueryAndBodyUserIdOverrides() throws Exception {
        authenticateAs(user(42L, "traveler", User.Role.USER));
        when(recommendationService.recommendSpotsForUser(eq(42L), nullable(RecommendationContext.class)))
                .thenReturn(List.of(scenicSpot()));

        mockMvc.perform(post("/api/spots/recommendations/me")
                        .header("Authorization", "Bearer " + USER_TOKEN)
                        .param("locale", "bo")
                        .param("userId", "999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":999,\"season\":\"AUTUMN\",\"budget\":300}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11))
                .andExpect(jsonPath("$[0].name").value("bo-name"));

        ArgumentCaptor<RecommendationContext> contextCaptor = ArgumentCaptor.forClass(RecommendationContext.class);
        verify(recommendationService).recommendSpotsForUser(eq(42L), contextCaptor.capture());
        assertThat(contextCaptor.getValue().getSeason()).isEqualTo("AUTUMN");
        assertThat(contextCaptor.getValue().getBudget()).isEqualTo(300);
        verifyNoMoreInteractions(recommendationService);
    }

    @Test
    void getMyRecommendationsRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/spots/recommendations/me")
                        .param("userId", "999"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication required"));

        verifyNoInteractions(recommendationService);
    }

    @Test
    void postMyRecommendationsRequiresAuthentication() throws Exception {
        mockMvc.perform(post("/api/spots/recommendations/me")
                        .param("userId", "999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userId\":999,\"season\":\"AUTUMN\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Unauthorized"))
                .andExpect(jsonPath("$.message").value("Authentication required"));

        verifyNoInteractions(recommendationService);
    }

    @Test
    void legacyGetRecommendationsAllowsAdminToUseRequestedUserId() throws Exception {
        authenticateAs(user(7L, "admin", User.Role.ADMIN));
        when(recommendationService.recommendSpotsForUser(eq(99L), nullable(RecommendationContext.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/spots/recommendations")
                        .header("Authorization", "Bearer " + USER_TOKEN)
                        .param("userId", "99")
                        .param("season", "SUMMER"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        ArgumentCaptor<RecommendationContext> contextCaptor = ArgumentCaptor.forClass(RecommendationContext.class);
        verify(recommendationService).recommendSpotsForUser(eq(99L), contextCaptor.capture());
        assertThat(contextCaptor.getValue().getSeason()).isEqualTo("SUMMER");
        verifyNoMoreInteractions(recommendationService);
    }

    @Test
    void legacyPostRecommendationsAllowsAdminToUseRequestedUserId() throws Exception {
        authenticateAs(user(7L, "admin", User.Role.ADMIN));
        when(recommendationService.recommendSpotsForUser(eq(99L), nullable(RecommendationContext.class)))
                .thenReturn(List.of());

        mockMvc.perform(post("/api/spots/recommendations")
                        .header("Authorization", "Bearer " + USER_TOKEN)
                        .param("userId", "99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"season\":\"SPRING\"}"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        ArgumentCaptor<RecommendationContext> contextCaptor = ArgumentCaptor.forClass(RecommendationContext.class);
        verify(recommendationService).recommendSpotsForUser(eq(99L), contextCaptor.capture());
        assertThat(contextCaptor.getValue().getSeason()).isEqualTo("SPRING");
        verifyNoMoreInteractions(recommendationService);
    }

    @Test
    void legacyRecommendationsRejectsDifferentRegularUser() throws Exception {
        authenticateAs(user(42L, "traveler", User.Role.USER));

        mockMvc.perform(get("/api/spots/recommendations")
                        .header("Authorization", "Bearer " + USER_TOKEN)
                        .param("userId", "99"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(recommendationService);
    }

    @Test
    void recommendationDebugRejectsAuthenticatedNonAdmin() throws Exception {
        authenticateAs(user(42L, "traveler", User.Role.USER));

        mockMvc.perform(get("/api/spots/recommendations/debug")
                        .header("Authorization", "Bearer " + USER_TOKEN)
                        .param("userId", "42"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(recommendationService);
    }

    private void authenticateAs(User appUser) {
        when(tokenRevocationService.isRevoked(USER_TOKEN)).thenReturn(false);
        when(jwtUtils.validateJwtToken(USER_TOKEN)).thenReturn(true);
        when(userSessionVersionService.tokenMatchesCurrentSession(USER_TOKEN)).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken(USER_TOKEN)).thenReturn(appUser.getUsername());
        when(userDetailsService.loadUserByUsername(appUser.getUsername()))
                .thenReturn(new org.springframework.security.core.userdetails.User(
                        appUser.getUsername(),
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_" + appUser.getRole().name()))));
        when(userRepository.findByUsername(appUser.getUsername())).thenReturn(Optional.of(appUser));
    }

    private static User user(Long id, String username, User.Role role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRole(role);
        user.setMustChangePassword(false);
        return user;
    }

    private static ScenicSpot scenicSpot() {
        ScenicSpot spot = new ScenicSpot();
        spot.setId(11L);
        spot.setName("Potala Palace");
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
        return spot;
    }
}
