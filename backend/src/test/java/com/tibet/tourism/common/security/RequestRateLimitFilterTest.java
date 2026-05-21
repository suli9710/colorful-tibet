package com.tibet.tourism.common.security;
import com.tibet.tourism.modules.user.domain.User;
import jakarta.servlet.http.Cookie;
import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.assertThat;

class RequestRateLimitFilterTest {

    private RequestRateLimitFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        ObjectProvider<RedisTemplate<String, Object>> provider = new ObjectProvider<>() {
            @Override
            public RedisTemplate<String, Object> getObject() {
                return null;
            }

            @Override
            public RedisTemplate<String, Object> getObject(Object... args) {
                return null;
            }

            @Override
            public RedisTemplate<String, Object> getIfAvailable() {
                return null;
            }

            @Override
            public RedisTemplate<String, Object> getIfUnique() {
                return null;
            }
        };
        filter = new RequestRateLimitFilter(provider, new TrustedProxyIpResolver());
        setField("enabled", true);
        setField("redisEnabled", false);
        setField("defaultRequests", 10);
        setField("defaultWindowSeconds", 60L);
        setField("authRequests", 3);
        setField("authWindowSeconds", 60L);
        setField("aiRequests", 2);
        setField("aiWindowSeconds", 600L);
        setField("guideChatRequests", 2);
        setField("guideChatWindowSeconds", 300L);
        setField("uploadRequests", 5);
        setField("uploadWindowSeconds", 60L);
        setField("adminRequests", 8);
        setField("adminWindowSeconds", 60L);
        setField("trustProxyHeaders", false);
        setField("trustedProxyCidrs", "");
    }

    private void setField(String name, Object value) throws Exception {
        Field field = RequestRateLimitFilter.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(filter, value);
    }

    @Test
    @DisplayName("requests within default limit pass through")
    void withinLimitPassesThrough() throws Exception {
        for (int i = 0; i < 10; i++) {
            MockHttpServletResponse response = doFilter(apiRequest("GET", "/api/spots"));
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
            assertThat(response.getHeader("X-RateLimit-Remaining")).isNotNull();
        }
    }

    @Test
    @DisplayName("requests exceeding default limit return 429")
    void exceedingLimitReturns429() throws Exception {
        MockHttpServletResponse blocked = null;
        for (int i = 0; i < 20; i++) {
            MockHttpServletResponse response = doFilter(apiRequest("GET", "/api/spots"));
            if (response.getStatus() == 429) {
                blocked = response;
                break;
            }
        }
        assertThat(blocked).isNotNull();
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getContentAsString()).contains("Too Many Requests");
        assertThat(blocked.getHeader("Retry-After")).isNotNull();
    }

    @Test
    @DisplayName("auth endpoints use stricter rate limit")
    void authEndpointsUseStricterLimit() throws Exception {
        for (int i = 0; i < 3; i++) {
            MockHttpServletResponse response = doFilter(apiRequest("POST", "/api/auth/login"));
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("3");
        }
        MockHttpServletResponse blocked = doFilter(apiRequest("POST", "/api/auth/login"));
        assertThat(blocked.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("AI route generation uses stricter limit")
    void aiEndpointsUseStricterLimit() throws Exception {
        for (int i = 0; i < 2; i++) {
            MockHttpServletResponse response = doFilter(apiRequest("POST", "/api/routes/generate"));
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("2");
        }
        MockHttpServletResponse blocked = doFilter(apiRequest("POST", "/api/routes/generate"));
        assertThat(blocked.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("AI guide chat uses stricter limit")
    void aiGuideChatUsesStricterLimit() throws Exception {
        for (int i = 0; i < 2; i++) {
            MockHttpServletResponse response = doFilter(apiRequest("POST", "/api/guide/chat"));
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("2");
        }
        MockHttpServletResponse blocked = doFilter(apiRequest("POST", "/api/guide/chat"));
        assertThat(blocked.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("upload endpoints use upload tier limit")
    void uploadEndpointsUseUploadLimit() throws Exception {
        for (int i = 0; i < 5; i++) {
            MockHttpServletResponse response = doFilter(apiRequest("POST", "/api/admin/upload-image"));
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("5");
        }
        MockHttpServletResponse blocked = doFilter(apiRequest("POST", "/api/admin/upload-image"));
        assertThat(blocked.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("admin endpoints use admin tier limit")
    void adminEndpointsUseAdminLimit() throws Exception {
        for (int i = 0; i < 8; i++) {
            MockHttpServletResponse response = doFilter(apiRequest("GET", "/api/admin/users"));
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("8");
        }
        MockHttpServletResponse blocked = doFilter(apiRequest("GET", "/api/admin/users"));
        assertThat(blocked.getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("OPTIONS requests bypass rate limiting")
    void optionsRequestsBypassRateLimiting() throws Exception {
        for (int i = 0; i < 30; i++) {
            MockHttpServletResponse response = doFilter(apiRequest("OPTIONS", "/api/spots"));
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("non-API paths bypass rate limiting")
    void nonApiPathsBypassRateLimiting() throws Exception {
        for (int i = 0; i < 30; i++) {
            MockHttpServletResponse response = doFilter(apiRequest("GET", "/images/photo.jpg"));
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("rate limit headers are set on every response")
    void rateLimitHeadersAreSet() throws Exception {
        MockHttpServletResponse response = doFilter(apiRequest("GET", "/api/spots"));
        assertThat(response.getHeader("X-RateLimit-Limit")).isNotNull();
        assertThat(response.getHeader("X-RateLimit-Remaining")).isNotNull();
        assertThat(response.getHeader("X-RateLimit-Reset")).isNotNull();
    }

    @Test
    @DisplayName("disabled rate limiting passes all requests")
    void disabledRateLimitingPassesAll() throws Exception {
        setField("enabled", false);
        for (int i = 0; i < 50; i++) {
            MockHttpServletResponse response = doFilter(apiRequest("POST", "/api/auth/login"));
            assertThat(response.getStatus()).isEqualTo(200);
        }
    }

    @Test
    @DisplayName("sliding window prevents 2x burst at window boundary")
    void slidingWindowPreventsBoundaryBurst() throws Exception {
        setField("defaultRequests", 10);
        setField("defaultWindowSeconds", 2L);

        for (int i = 0; i < 10; i++) {
            doFilter(apiRequest("GET", "/api/spots"));
        }

        MockHttpServletResponse firstAfterWindow = doFilter(apiRequest("GET", "/api/spots"));
        assertThat(firstAfterWindow.getHeader("X-RateLimit-Remaining")).isNotNull();
    }

    @Test
    @DisplayName("clients with different IPs get independent rate limits")
    void differentClientsIndependent() throws Exception {
        // Exhaust client A's limit
        for (int i = 0; i < 10; i++) {
            doFilter(apiRequestFromIp("GET", "/api/spots", "10.0.0.1"));
        }
        assertThat(doFilter(apiRequestFromIp("GET", "/api/spots", "10.0.0.1")).getStatus()).isEqualTo(429);

        // Client B should still be fine
        assertThat(doFilter(apiRequestFromIp("GET", "/api/spots", "10.0.0.2")).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("spoofed forwarded headers are ignored when remote address is not a trusted proxy")
    void spoofedForwardedHeadersDoNotBypassDirectBackendRateLimit() throws Exception {
        setField("trustProxyHeaders", true);
        setField("trustedProxyCidrs", "172.18.0.0/16");

        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest request = apiRequestFromIp("GET", "/api/spots", "203.0.113.10");
            request.addHeader("X-Forwarded-For", "198.51.100." + i);
            assertThat(doFilter(request).getStatus()).isEqualTo(200);
        }

        MockHttpServletRequest blockedRequest = apiRequestFromIp("GET", "/api/spots", "203.0.113.10");
        blockedRequest.addHeader("X-Forwarded-For", "198.51.100.200");
        assertThat(doFilter(blockedRequest).getStatus()).isEqualTo(429);
    }

    @Test
    @DisplayName("trusted proxies can forward distinct client identities")
    void trustedProxyForwardedClientIdentityIsUsed() throws Exception {
        setField("defaultRequests", 1);
        setField("trustProxyHeaders", true);
        setField("trustedProxyCidrs", "172.18.0.0/16");

        MockHttpServletRequest clientA = apiRequestFromIp("GET", "/api/spots", "172.18.0.20");
        clientA.addHeader("X-Forwarded-For", "198.51.100.20");
        assertThat(doFilter(clientA).getStatus()).isEqualTo(200);

        MockHttpServletRequest clientABlocked = apiRequestFromIp("GET", "/api/spots", "172.18.0.20");
        clientABlocked.addHeader("X-Forwarded-For", "198.51.100.20");
        assertThat(doFilter(clientABlocked).getStatus()).isEqualTo(429);

        MockHttpServletRequest clientB = apiRequestFromIp("GET", "/api/spots", "172.18.0.20");
        clientB.addHeader("X-Forwarded-For", "198.51.100.21");
        assertThat(doFilter(clientB).getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("clients with different auth cookies get independent rate limits")
    void differentAuthCookiesIndependent() throws Exception {
        // Exhaust user A's limit
        for (int i = 0; i < 10; i++) {
            doFilter(apiRequestWithCookie("GET", "/api/spots", "jwt-token-user-a"));
        }
        assertThat(doFilter(apiRequestWithCookie("GET", "/api/spots", "jwt-token-user-a")).getStatus()).isEqualTo(429);

        // User B should still be fine
        assertThat(doFilter(apiRequestWithCookie("GET", "/api/spots", "jwt-token-user-b")).getStatus()).isEqualTo(200);
    }

    private MockHttpServletRequest apiRequest(String method, String path) {
        return apiRequestFromIp(method, path, "127.0.0.1");
    }

    private MockHttpServletRequest apiRequestFromIp(String method, String path, String ip) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        request.setRemoteAddr(ip);
        return request;
    }

    private MockHttpServletRequest apiRequestWithCookie(String method, String path, String token) {
        MockHttpServletRequest request = apiRequestFromIp(method, path, "10.0.0.1");
        request.setCookies(new Cookie(CookieAuthConstants.AUTH_COOKIE_NAME, token));
        return request;
    }

    private MockHttpServletResponse doFilter(MockHttpServletRequest request) throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilterInternal(request, response, new MockFilterChain());
        return response;
    }
}
