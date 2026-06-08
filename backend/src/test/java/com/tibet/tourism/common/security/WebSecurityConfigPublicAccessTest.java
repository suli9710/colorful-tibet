package com.tibet.tourism.common.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.containsString;

import com.tibet.tourism.modules.user.infra.UserRepository;
import jakarta.servlet.http.Cookie;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.User;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = WebSecurityConfigPublicAccessTest.ProbeController.class)
@Import({
        WebSecurityConfig.class,
        AuthEntryPointJwt.class,
        CsrfCookieFilter.class,
        TrustedProxyIpResolver.class,
        WebSecurityConfigPublicAccessTest.ProbeController.class
})
class WebSecurityConfigPublicAccessTest {

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
    private UserRepository userRepository;

    @ParameterizedTest
    @MethodSource("publicReadRequests")
    void publicReadEndpointsAllowAnonymous(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @MethodSource("publicPostRequests")
    void publicAuthPostsAllowAnonymous(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk());
    }

    @ParameterizedTest
    @MethodSource("protectedRequestsInsidePublicNamespaces")
    void protectedEndpointsInsidePublicNamespacesRequireAuthentication(MockHttpServletRequestBuilder request)
            throws Exception {
        mockMvc.perform(request.contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("deniedUploadRequests")
    void privateUploadsAreNotPublicForAnonymousRequests(MockHttpServletRequestBuilder request) throws Exception {
        mockMvc.perform(request)
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @MethodSource("deniedUploadRequests")
    void privateUploadsDenyAuthenticatedUsers(MockHttpServletRequestBuilder request) throws Exception {
        when(jwtUtils.validateJwtToken("valid-token")).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken("valid-token")).thenReturn("traveler");
        when(userSessionVersionService.tokenMatchesCurrentSession("valid-token")).thenReturn(true);
        when(userDetailsService.loadUserByUsername("traveler")).thenReturn(
                User.withUsername("traveler").password("unused").roles("USER").build());

        mockMvc.perform(request.cookie(new Cookie(CookieAuthConstants.AUTH_COOKIE_NAME, "valid-token")))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminEndpointForNonAdminReturnsGenericJsonForbidden() throws Exception {
        when(jwtUtils.validateJwtToken("valid-token")).thenReturn(true);
        when(jwtUtils.getUserNameFromJwtToken("valid-token")).thenReturn("traveler");
        when(userSessionVersionService.tokenMatchesCurrentSession("valid-token")).thenReturn(true);
        when(userDetailsService.loadUserByUsername("traveler")).thenReturn(
                User.withUsername("traveler").password("unused").roles("USER").build());

        mockMvc.perform(get("/api/admin/users")
                        .cookie(new Cookie(CookieAuthConstants.AUTH_COOKIE_NAME, "valid-token")))
                .andExpect(status().isForbidden())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    private static Stream<MockHttpServletRequestBuilder> publicReadRequests() {
        return Stream.of(
                get("/api/spots"),
                get("/api/spots/search"),
                get("/api/spots/heatmap"),
                get("/api/spots/1"),
                get("/api/spots/1/similar"),
                get("/api/news"),
                get("/api/heritage"),
                get("/api/heritage/1"),
                get("/api/heritage/1/comments"),
                get("/api/heritage/1/inheritors"),
                get("/api/heritage/1/events"),
                get("/api/heritage/events/upcoming"),
                get("/api/tibet-specialty/culture-tips"),
                get("/api/tibet-specialty/phrasebook"),
                get("/api/tibet-specialty/sustainable-options"),
                get("/api/routes/shared"),
                get("/api/routes/shared/1"),
                get("/api/routes/shared/1/comments"),
                get("/api/carousels"),
                get("/api/hotel-bookings/hotels"),
                get("/api/hotel-bookings/hotels/1"),
                get("/api/hotel-bookings/room-types/1"),
                get("/api/comments/spot/1"),
                get("/api/community/questions"),
                get("/api/community/questions/1"),
                get("/api/community/questions/1/answers"),
                get("/api/community/questions/1/like-status"),
                get("/images/public.jpg"),
                get("/uploads/public.jpg"));
    }

    private static Stream<MockHttpServletRequestBuilder> publicPostRequests() {
        return Stream.of(
                post("/api/auth/login"),
                post("/api/auth/register"),
                post("/api/guide/chat"));
    }

    private static Stream<MockHttpServletRequestBuilder> protectedRequestsInsidePublicNamespaces() {
        return Stream.of(
                post("/api/auth/logout"),
                post("/api/news"),
                put("/api/news/1"),
                post("/api/heritage"),
                post("/api/heritage/1/like"),
                get("/api/heritage/1/like-status"),
                post("/api/heritage/1/comments"),
                put("/api/heritage/1"),
                delete("/api/heritage/1/comments/100"),
                post("/api/payments/callbacks/mock"),
                post("/api/hotel-bookings/hotels"),
                post("/api/hotel-bookings/room-types/1"),
                post("/api/routes/shared/1/comments"),
                delete("/api/routes/shared/1"),
                get("/api/routes/shared/1/like-status"),
                post("/api/community/questions"),
                delete("/api/community/questions/1"),
                post("/api/comments/spot/1"));
    }

    private static Stream<MockHttpServletRequestBuilder> deniedUploadRequests() {
        return Stream.of(
                get("/uploads/private"),
                get("/uploads/private/order-voucher.jpg"));
    }

    @RestController
    public static class ProbeController {

        @GetMapping({
                "/api/spots",
                "/api/spots/search",
                "/api/spots/heatmap",
                "/api/spots/1",
                "/api/spots/1/similar",
                "/api/news",
                "/api/heritage",
                "/api/heritage/1",
                "/api/heritage/1/comments",
                "/api/heritage/1/inheritors",
                "/api/heritage/1/events",
                "/api/heritage/events/upcoming",
                "/api/tibet-specialty/culture-tips",
                "/api/tibet-specialty/phrasebook",
                "/api/tibet-specialty/sustainable-options",
                "/api/routes/shared",
                "/api/routes/shared/1",
                "/api/routes/shared/1/comments",
                "/api/carousels",
                "/api/hotel-bookings/hotels",
                "/api/hotel-bookings/hotels/1",
                "/api/hotel-bookings/room-types/1",
                "/api/comments/spot/1",
                "/api/community/questions",
                "/api/community/questions/1",
                "/api/community/questions/1/answers",
                "/api/community/questions/1/like-status",
                "/images/public.jpg",
                "/uploads/public.jpg"
        })
        public Map<String, String> publicRead() {
            return Map.of("status", "ok");
        }

        @PostMapping({
                "/api/auth/login",
                "/api/auth/logout",
                "/api/auth/register",
                "/api/guide/chat",
                "/api/payments/callbacks/mock",
                "/api/news",
                "/api/heritage",
                "/api/heritage/1/like",
                "/api/heritage/1/comments",
                "/api/hotel-bookings/hotels",
                "/api/hotel-bookings/room-types/1",
                "/api/routes/shared/1/comments",
                "/api/community/questions",
                "/api/comments/spot/1"
        })
        public Map<String, String> postProbe() {
            return Map.of("status", "ok");
        }

        @PutMapping({
                "/api/news/1",
                "/api/heritage/1"
        })
        public Map<String, String> putProbe() {
            return Map.of("status", "ok");
        }

        @DeleteMapping({
                "/api/routes/shared/1",
                "/api/community/questions/1",
                "/api/heritage/1/comments/100"
        })
        public Map<String, String> deleteProbe() {
            return Map.of("status", "ok");
        }

        @GetMapping({
                "/api/routes/shared/1/like-status",
                "/api/heritage/1/like-status",
                "/uploads/private/order-voucher.jpg"
        })
        public Map<String, String> protectedReadProbe() {
            return Map.of("status", "ok");
        }
    }
}
