package com.tibet.tourism.common.security;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import jakarta.servlet.http.Cookie;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import static org.assertj.core.api.Assertions.assertThat;

class RequestRateLimitFilterTest {

    private RequestRateLimitFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        filter = new RequestRateLimitFilter(provider(null), new TrustedProxyIpResolver(), provider(null));
        configureDefaults(filter);
    }

    private void setField(String name, Object value) throws Exception {
        setField(filter, name, value);
    }

    private void setField(RequestRateLimitFilter targetFilter, String name, Object value) throws Exception {
        Field field = RequestRateLimitFilter.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(targetFilter, value);
    }

    private void configureDefaults(RequestRateLimitFilter targetFilter) throws Exception {
        setField(targetFilter, "enabled", true);
        setField(targetFilter, "redisEnabled", false);
        setField(targetFilter, "defaultRequests", 10);
        setField(targetFilter, "defaultWindowSeconds", 60L);
        setField(targetFilter, "authRequests", 3);
        setField(targetFilter, "authWindowSeconds", 60L);
        setField(targetFilter, "aiRequests", 2);
        setField(targetFilter, "aiWindowSeconds", 600L);
        setField(targetFilter, "guideChatRequests", 2);
        setField(targetFilter, "guideChatWindowSeconds", 300L);
        setField(targetFilter, "uploadRequests", 5);
        setField(targetFilter, "uploadWindowSeconds", 60L);
        setField(targetFilter, "adminRequests", 8);
        setField(targetFilter, "adminWindowSeconds", 60L);
        setField(targetFilter, "trustProxyHeaders", false);
        setField(targetFilter, "trustedProxyCidrs", "");
    }

    private static <T> ObjectProvider<T> provider(T value) {
        return new ObjectProvider<>() {
            @Override
            public T getObject() {
                return value;
            }

            @Override
            public T getObject(Object... args) {
                return value;
            }

            @Override
            public T getIfAvailable() {
                return value;
            }

            @Override
            public T getIfUnique() {
                return value;
            }
        };
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
    @DisplayName("AI route job polling uses default read limit")
    void aiJobPollingUsesDefaultLimit() throws Exception {
        for (int i = 0; i < 3; i++) {
            MockHttpServletResponse response = doFilter(apiRequest("GET", "/api/routes/generate/jobs/job-1"));
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
        }
    }

    @Test
    @DisplayName("AI route job stream uses default read limit")
    void aiJobStreamUsesDefaultLimit() throws Exception {
        MockHttpServletResponse response = doFilter(apiRequest("GET", "/api/routes/generate/jobs/job-1/stream"));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-RateLimit-Limit")).isEqualTo("10");
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
    @DisplayName("AI guide chat limit binds from rate-limit properties")
    void aiGuideChatLimitBindsFromRateLimitProperties() {
        new ApplicationContextRunner()
                .withBean(TrustedProxyIpResolver.class)
                .withBean(RequestRateLimitFilter.class)
                .withPropertyValues(
                        "app.security.rate-limit.redis-enabled=false",
                        "app.security.rate-limit.guide-chat.requests=2",
                        "app.security.rate-limit.guide-chat.window-seconds=60")
                .run(context -> {
                    RequestRateLimitFilter configuredFilter = context.getBean(RequestRateLimitFilter.class);

                    assertThat(doFilter(configuredFilter, apiRequest("POST", "/api/guide/chat")).getStatus())
                            .isEqualTo(200);
                    assertThat(doFilter(configuredFilter, apiRequest("POST", "/api/guide/chat")).getStatus())
                            .isEqualTo(200);

                    MockHttpServletResponse blocked =
                            doFilter(configuredFilter, apiRequest("POST", "/api/guide/chat"));
                    assertThat(blocked.getStatus()).isEqualTo(429);
                    assertThat(blocked.getHeader("X-RateLimit-Limit")).isEqualTo("2");
                });
    }

    @Test
    @DisplayName("AI guide chat limit remains compatible with legacy guide-chat properties")
    void aiGuideChatLimitBindsFromLegacyGuideChatProperties() {
        new ApplicationContextRunner()
                .withBean(TrustedProxyIpResolver.class)
                .withBean(RequestRateLimitFilter.class)
                .withPropertyValues(
                        "app.security.rate-limit.redis-enabled=false",
                        "app.security.guide-chat.window-limit=2",
                        "app.security.guide-chat.window-seconds=60")
                .run(context -> {
                    RequestRateLimitFilter configuredFilter = context.getBean(RequestRateLimitFilter.class);

                    assertThat(doFilter(configuredFilter, apiRequest("POST", "/api/guide/chat")).getStatus())
                            .isEqualTo(200);
                    assertThat(doFilter(configuredFilter, apiRequest("POST", "/api/guide/chat")).getStatus())
                            .isEqualTo(200);

                    MockHttpServletResponse blocked =
                            doFilter(configuredFilter, apiRequest("POST", "/api/guide/chat"));
                    assertThat(blocked.getStatus()).isEqualTo(429);
                    assertThat(blocked.getHeader("X-RateLimit-Limit")).isEqualTo("2");
                });
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
    @DisplayName("redis script repairs legacy keys without TTL")
    void redisScriptRepairsLegacyKeysWithoutTtl() {
        assertThat(RequestRateLimitFilter.INCREMENT_WITH_EXPIRE_LUA)
                .contains("redis.call('TTL', KEYS[1]) < 0");
    }

    @Test
    @DisplayName("Redis failures activate observable in-memory fallback")
    void redisFailuresActivateObservableFallback() throws Exception {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RequestRateLimitFilter redisFilter = redisBackedFilter(new FailingRedisTemplate(), meterRegistry);

        MockHttpServletResponse response = doFilter(redisFilter, apiRequest("GET", "/api/spots"));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(redisFilter.isRedisFallbackActive()).isTrue();
        assertThat(redisFilter.redisFallbackEvents()).isEqualTo(1);
        assertThat(meterRegistry.find("app.security.rate.limit.redis.fallback.events").counter()).isNotNull();
        assertThat(meterRegistry.find("app.security.rate.limit.redis.fallback.events").counter().count())
                .isEqualTo(1.0);
        assertThat(meterRegistry.find("app.security.rate.limit.redis.fallback.active").gauge()).isNotNull();
        assertThat(meterRegistry.find("app.security.rate.limit.redis.fallback.active").gauge().value())
                .isEqualTo(1.0);
    }

    @Test
    @DisplayName("Redis success clears fallback status after recovery")
    void redisSuccessClearsFallbackStatusAfterRecovery() throws Exception {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RequestRateLimitFilter redisFilter = redisBackedFilter(new RecoveringRedisTemplate(), meterRegistry);

        assertThat(doFilter(redisFilter, apiRequest("GET", "/api/spots")).getStatus()).isEqualTo(200);
        assertThat(redisFilter.isRedisFallbackActive()).isTrue();

        assertThat(doFilter(redisFilter, apiRequest("GET", "/api/spots")).getStatus()).isEqualTo(200);

        assertThat(redisFilter.isRedisFallbackActive()).isFalse();
        assertThat(redisFilter.redisFallbackEvents()).isEqualTo(1);
        assertThat(meterRegistry.find("app.security.rate.limit.redis.fallback.events").counter().count())
                .isEqualTo(1.0);
        assertThat(meterRegistry.find("app.security.rate.limit.redis.fallback.active").gauge().value())
                .isEqualTo(0.0);
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
    @DisplayName("unverified Bearer tokens do not bypass IP rate limits")
    void unverifiedBearerTokensDoNotBypassIpLimit() throws Exception {
        setField("defaultRequests", 1);

        assertThat(doFilter(apiRequestWithBearer("GET", "/api/spots", "jwt-token-user-a")).getStatus())
                .isEqualTo(200);
        assertThat(doFilter(apiRequestWithBearer("GET", "/api/spots", "jwt-token-user-b")).getStatus())
                .isEqualTo(429);
    }

    @Test
    @DisplayName("client-controlled anonymous headers do not bypass IP rate limits")
    void clientControlledAnonymousHeadersDoNotBypassIpLimit() throws Exception {
        setField("defaultRequests", 1);

        assertThat(doFilter(apiRequestWithFingerprint("GET", "/api/spots", "10.0.0.1", "ColorfulTest/1.0"))
                .getStatus()).isEqualTo(200);
        assertThat(doFilter(apiRequestWithFingerprint("GET", "/api/spots", "10.0.0.1", "ColorfulTest/2.0"))
                .getStatus()).isEqualTo(429);
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
    @DisplayName("unverified auth cookies do not bypass IP rate limits")
    void unverifiedAuthCookiesDoNotBypassIpLimit() throws Exception {
        setField("defaultRequests", 1);

        assertThat(doFilter(apiRequestWithCookieAndBearer(
                "GET", "/api/spots", "jwt-token-user-a", "shared-bearer-token")).getStatus()).isEqualTo(200);
        assertThat(doFilter(apiRequestWithCookieAndBearer(
                "GET", "/api/spots", "jwt-token-user-b", "shared-bearer-token")).getStatus()).isEqualTo(429);
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

    private MockHttpServletRequest apiRequestWithFingerprint(String method, String path, String ip, String userAgent) {
        MockHttpServletRequest request = apiRequestFromIp(method, path, ip);
        request.addHeader("User-Agent", userAgent);
        request.addHeader("Accept-Language", "zh-CN");
        return request;
    }

    private MockHttpServletRequest apiRequestWithBearer(String method, String path, String token) {
        MockHttpServletRequest request = apiRequestWithFingerprint(method, path, "10.0.0.1", "ColorfulTest/1.0");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        return request;
    }

    private MockHttpServletRequest apiRequestWithCookie(String method, String path, String token) {
        MockHttpServletRequest request = apiRequestFromIp(method, path, "10.0.0.1");
        request.setCookies(new Cookie(CookieAuthConstants.AUTH_COOKIE_NAME, token));
        return request;
    }

    private MockHttpServletRequest apiRequestWithCookieAndBearer(
            String method, String path, String cookieToken, String bearerToken) {
        MockHttpServletRequest request = apiRequestWithCookie(method, path, cookieToken);
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
        return request;
    }

    private MockHttpServletResponse doFilter(MockHttpServletRequest request) throws Exception {
        return doFilter(filter, request);
    }

    private MockHttpServletResponse doFilter(RequestRateLimitFilter targetFilter, MockHttpServletRequest request)
            throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        targetFilter.doFilterInternal(request, response, new MockFilterChain());
        return response;
    }

    private RequestRateLimitFilter redisBackedFilter(StringRedisTemplate redisTemplate,
                                                     SimpleMeterRegistry meterRegistry) throws Exception {
        RequestRateLimitFilter targetFilter = new RequestRateLimitFilter(
                provider(redisTemplate),
                new TrustedProxyIpResolver(),
                provider(meterRegistry));
        configureDefaults(targetFilter);
        setField(targetFilter, "redisEnabled", true);
        return targetFilter;
    }

    private static final class FailingRedisTemplate extends StringRedisTemplate {
        @Override
        public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
            throw new IllegalStateException("redis unavailable");
        }
    }

    private static final class RecoveringRedisTemplate extends StringRedisTemplate {
        private int attempts;

        @Override
        @SuppressWarnings("unchecked")
        public <T> T execute(RedisScript<T> script, List<String> keys, Object... args) {
            attempts++;
            if (attempts == 1) {
                throw new IllegalStateException("redis unavailable");
            }
            return (T) Long.valueOf(1);
        }

        @Override
        public Long getExpire(String key, TimeUnit timeUnit) {
            return 60L;
        }
    }
}
