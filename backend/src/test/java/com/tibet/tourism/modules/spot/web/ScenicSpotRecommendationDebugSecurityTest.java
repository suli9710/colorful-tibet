package com.tibet.tourism.modules.spot.web;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import com.tibet.tourism.modules.spot.application.CompanionInferenceService;
import com.tibet.tourism.modules.spot.application.ScenicSpotService;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

@WebMvcTest(controllers = ScenicSpotController.class)
@Import({
        WebSecurityConfig.class,
        AuthEntryPointJwt.class,
        CsrfCookieFilter.class,
        TrustedProxyIpResolver.class
})
class ScenicSpotRecommendationDebugSecurityTest {

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

    @ParameterizedTest
    @MethodSource("debugRequests")
    void recommendationDebugRejectsAuthenticatedNonAdmin(MockHttpServletRequestBuilder request) throws Exception {
        authenticateAsRegularUser();

        mockMvc.perform(request.header("Authorization", "Bearer " + USER_TOKEN))
                .andExpect(status().isForbidden());

        verifyNoInteractions(recommendationService);
    }

    private static Stream<MockHttpServletRequestBuilder> debugRequests() {
        return Stream.of(
                get("/api/spots/recommendations/debug").param("userId", "7"),
                post("/api/spots/recommendations/debug")
                        .param("userId", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"));
    }

    private void authenticateAsRegularUser() {
        when(tokenRevocationService.isRevoked(USER_TOKEN)).thenReturn(false);
        when(jwtUtils.validateJwtToken(USER_TOKEN)).thenReturn(true);
        when(userSessionVersionService.tokenMatchesCurrentSession(USER_TOKEN)).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken(USER_TOKEN)).thenReturn("traveler");
        when(userDetailsService.loadUserByUsername("traveler"))
                .thenReturn(new org.springframework.security.core.userdetails.User(
                        "traveler",
                        "n/a",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));
    }
}
