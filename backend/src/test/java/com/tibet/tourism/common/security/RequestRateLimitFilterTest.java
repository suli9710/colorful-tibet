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

    @Test
    void inMemoryFallbackHasHardMaximumSize() {
        assertThat(filter.inMemoryMaximumSize()).isEqualTo(20_000);
    }

    @Test
    void clientErrorReportsHaveDedicatedAbuseLimit() throws Exception {
        assertThat(doFilter(apiRequest("POST", "/api/client-errors")).getStatus()).isEqualTo(200);
        assertThat(doFilter(apiRequest("POST", "/api/client-errors")).getStatus()).isEqualTo(200);
        assertThat(doFilter(apiRequest("POST", "/api/client-errors")).getStatus()).isEqualTo(429);
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
        setField(targetFilter, "redisFailClosed", false);
        setField(targetFilter, "defaultRequests", 10);
        setField(targetFilter, "defaultWindowSeconds", 60L);
        setField(targetFilter, "authRequests", 3);
        setField(targetFilter, "authWindowSeconds", 60L);
        setField(targetFilter, "registerRequests", 2);
        setField(targetFilter, "registerWindowSeconds", 3600L);
        setField(targetFilter, "aiRequests", 2);
        setField(targetFilter, "aiWindowSeconds", 600L);
        setField(targetFilter, "guideChatRequests", 2);
        setField(targetFilter, "guideChatWindowSeconds", 300L);
        setField(targetFilter, "uploadRequests", 5);
        setField(targetFilter, "uploadWindowSeconds", 60L);
        setField(targetFilter, "clientErrorRequests", 2);
        setField(targetFilter, "clientErrorWindowSeconds", 60L);
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
    @DisplayName("Redis failures fail closed for sensitive routes when strict mode is enabled")
    void redisFailuresFailClosedForSensitiveRoutesWhenStrict() throws Exception {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        RequestRateLimitFilter redisFilter = redisBackedFilter(new FailingRedisTemplate(), meterRegistry);
        setField(redisFilter, "redisFailClosed", true);

        MockHttpServletResponse response = doFilter(redisFilter, apiRequest("POST", "/api/auth/login"));

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("Rate limit verification is temporarily unavailable");
        assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        assertThat(redisFilter.isRedisFallbackActive()).isTrue();
        assertThat(redisFilter.redisFallbackEvents()).isEqualTo(1);
    }

    @Test
    @DisplayName("Redis failures fail closed for order mutations when strict mode is enabled")
    void redisFailuresFailClosedForOrderMutationsWhenStrict() throws Exception {
        RequestRateLimitFilter redisFilter = redisBackedFilter(new FailingRedisTemplate(), new SimpleMeterRegistry());
        setField(redisFilter, "redisFailClosed", true);

        MockHttpServletResponse createOrder = doFilter(redisFilter, apiRequest("POST", "/api/orders"));
        MockHttpServletResponse paymentCallback =
                doFilter(redisFilter, apiRequest("POST", "/api/payments/callbacks/mock"));

        assertThat(createOrder.getStatus()).isEqualTo(503);
        assertThat(createOrder.getContentAsString())
                .contains("Rate limit verification is temporarily unavailable");
        assertThat(paymentCallback.getStatus()).isEqualTo(503);
        assertThat(paymentCallback.getContentAsString())
                .contains("Rate limit verification is temporarily unavailable");
    }

    @Test
    @DisplayName("Redis failures fail closed for every unmatched API mutation method when strict mode is enabled")
    void redisFailuresFailClosedForUnmatchedApiMutationMethodsWhenStrict() throws Exception {
        RequestRateLimitFilter redisFilter = redisBackedFilter(new FailingRedisTemplate(), new SimpleMeterRegistry());
        setField(redisFilter, "redisFailClosed", true);

        for (String method : List.of("POST", "PUT", "PATCH", "DELETE")) {
            MockHttpServletResponse response =
                    doFilter(redisFilter, apiRequest(method, "/api/community/posts/42"));

            assertThat(response.getStatus()).isEqualTo(503);
            assertThat(response.getContentAsString())
                    .contains("Rate limit verification is temporarily unavailable");
            assertThat(response.getHeader("Retry-After")).isEqualTo("60");
        }
    }

    @Test
    @DisplayName("Redis failures fail closed for hotel inquiry mutations when strict mode is enabled")
    void redisFailuresFailClosedForHotelInquiryMutationsWhenStrict() throws Exception {
        RequestRateLimitFilter redisFilter = redisBackedFilter(new FailingRedisTemplate(), new SimpleMeterRegistry());
        setField(redisFilter, "redisFailClosed", true);

        MockHttpServletResponse response = doFilter(redisFilter, apiRequest("POST", "/api/hotel-bookings"));

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString())
                .contains("Rate limit verification is temporarily unavailable");
    }

    @Test
    @DisplayName("missing Redis backend fails closed for sensitive routes when strict mode is enabled")
    void missingRedisBackendFailsClosedForSensitiveRoutesWhenStrict() throws Exception {
        setField("redisEnabled", true);
        setField("redisFailClosed", true);

        MockHttpServletResponse response = doFilter(apiRequest("POST", "/api/auth/login"));

        assertThat(response.getStatus()).isEqualTo(503);
        assertThat(response.getContentAsString()).contains("Rate limit verification is temporarily unavailable");
        assertThat(filter.isRedisFallbackActive()).isTrue();
    }

    @Test
    @DisplayName("Redis failures fall back for sensitive mutations when strict mode is disabled")
    void redisFailuresFallbackForSensitiveMutationsWhenStrictModeDisabled() throws Exception {
        RequestRateLimitFilter redisFilter = redisBackedFilter(new FailingRedisTemplate(), new SimpleMeterRegistry());
        setField(redisFilter, "redisFailClosed", false);

        MockHttpServletResponse response = doFilter(redisFilter, apiRequest("POST", "/api/orders"));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(redisFilter.isRedisFallbackActive()).isTrue();
        assertThat(redisFilter.redisFallbackEvents()).isEqualTo(1);
    }

    @Test
    @DisplayName("disabled rate limiting bypasses Redis strict fail-closed")
    void disabledRateLimitingBypassesRedisStrictFailClosed() throws Exception {
        RequestRateLimitFilter redisFilter = redisBackedFilter(new FailingRedisTemplate(), new SimpleMeterRegistry());
        setField(redisFilter, "redisFailClosed", true);
        setField(redisFilter, "enabled", false);

        MockHttpServletResponse response = doFilter(redisFilter, apiRequest("POST", "/api/auth/login"));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader("X-RateLimit-Limit")).isNull();
        assertThat(redisFilter.isRedisFallbackActive()).isFalse();
        assertThat(redisFilter.redisFallbackEvents()).isZero();
    }

    @Test
    @DisplayName("Redis failures keep default reads available when strict mode only guards sensitive routes")
    void redisFailuresKeepDefaultReadsAvailableWhenStrict() throws Exception {
        RequestRateLimitFilter redisFilter = redisBackedFilter(new FailingRedisTemplate(), new SimpleMeterRegistry());
        setField(redisFilter, "redisFailClosed", true);

        MockHttpServletResponse response = doFilter(redisFilter, apiRequest("GET", "/api/spots"));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(redisFilter.isRedisFallbackActive()).isTrue();
    }

    @Test
    @DisplayName("Redis failures do not intercept actuator health checks when strict mode is enabled")
    void redisFailuresDoNotInterceptActuatorHealthChecksWhenStrict() throws Exception {
        RequestRateLimitFilter redisFilter = redisBackedFilter(new FailingRedisTemplate(), new SimpleMeterRegistry());
        setField(redisFilter, "redisFailClosed", true);

        for (int i = 0; i < 20; i++) {
            MockHttpServletResponse response = doFilter(redisFilter, apiRequest("GET", "/actuator/health"));

            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getHeader("X-RateLimit-Limit")).isNull();
        }
        assertThat(redisFilter.isRedisFallbackActive()).isFalse();
        assertThat(redisFilter.redisFallbackEvents()).isZero();
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
    @DisplayName("fail-closed rejects sensitive buckets when Redis is unavailable")
    void failClosedRejectsSensitiveBucketsOnRedisOutage() throws Exception {
        RequestRateLimitFilter redisFilter = redisBackedFilter(new FailingRedisTemplate(), new SimpleMeterRegistry());
        setField(redisFilter, "redisFailClosed", true);

        MockHttpServletResponse blocked = doFilter(redisFilter, apiRequest("POST", "/api/auth/login"));

        assertThat(blocked.getStatus()).isEqualTo(503);
        assertThat(blocked.getContentAsString()).contains("Rate limit verification is temporarily unavailable");
        assertThat(blocked.getHeader(HttpHeaders.RETRY_AFTER)).isEqualTo("60");
    }

    @Test
    @DisplayName("fail-closed still serves non-sensitive buckets from in-memory fallback")
    void failClosedStillServesNonSensitiveBucketsOnRedisOutage() throws Exception {
        RequestRateLimitFilter redisFilter = redisBackedFilter(new FailingRedisTemplate(), new SimpleMeterRegistry());
        setField(redisFilter, "redisFailClosed", true);

        MockHttpServletResponse response = doFilter(redisFilter, apiRequest("GET", "/api/spots"));

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(redisFilter.isRedisFallbackActive()).isTrue();
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
    @DisplayName("non-positive rate limit config falls back to conservative minimums")
    void nonPositiveLimitConfigUsesConservativeMinimums() throws Exception {
        setField("defaultRequests", 0);
        setField("defaultWindowSeconds", 0L);

        MockHttpServletResponse allowed = doFilter(apiRequest("GET", "/api/spots"));
        MockHttpServletResponse blocked = doFilter(apiRequest("GET", "/api/spots"));

        assertThat(allowed.getStatus()).isEqualTo(200);
        assertThat(allowed.getHeader("X-RateLimit-Limit")).isEqualTo("1");
        assertThat(blocked.getStatus()).isEqualTo(429);
        assertThat(blocked.getContentAsString()).contains("Request rate limit exceeded");
    }

    @Test
    @DisplayName("overly large window config is clamped instead of overflowing")
    void overlyLargeWindowConfigIsClamped() throws Exception {
        setField("defaultRequests", 1);
        setField("defaultWindowSeconds", Long.MAX_VALUE);

        MockHttpServletResponse allowed = doFilter(apiRequest("GET", "/api/spots"));

        assertThat(allowed.getStatus()).isEqualTo(200);
        assertThat(allowed.getHeader("X-RateLimit-Limit")).isEqualTo("1");
        assertThat(allowed.getHeader("X-RateLimit-Reset")).isNotNull();
    }

    @Test
    @DisplayName("sliding window still enforces the limit after a long idle gap")
    void slidingWindowEnforcesLimitAfterIdleGap() {
        // The window used to advance by exactly one window per call, so after idling N windows the
        // next N requests each re-entered the reset branch and were admitted regardless of the limit.
        RequestRateLimitFilter.LimitRule rule =
                new RequestRateLimitFilter.LimitRule("default", 2, 1_000L, false);
        RequestRateLimitFilter.RateWindow window = new RequestRateLimitFilter.RateWindow(0L);

        assertThat(window.tryAcquire(rule, 0L).allowed()).isTrue();
        assertThat(window.tryAcquire(rule, 10L).allowed()).isTrue();
        assertThat(window.tryAcquire(rule, 20L).allowed()).isFalse();

        // Idle for 10 whole windows, then burst: the first two are allowed for the fresh window and
        // the third must still be refused.
        long afterIdle = 10_000L;
        assertThat(window.tryAcquire(rule, afterIdle).allowed()).isTrue();
        assertThat(window.tryAcquire(rule, afterIdle + 1).allowed()).isTrue();
        assertThat(window.tryAcquire(rule, afterIdle + 2).allowed()).isFalse();
        assertThat(window.tryAcquire(rule, afterIdle + 3).allowed()).isFalse();
    }

    @Test
    @DisplayName("sliding window weights the previous window instead of resetting on the boundary")
    void slidingWindowWeightsPreviousWindowAcrossOneBoundary() {
        RequestRateLimitFilter.LimitRule rule =
                new RequestRateLimitFilter.LimitRule("default", 4, 1_000L, false);
        RequestRateLimitFilter.RateWindow window = new RequestRateLimitFilter.RateWindow(0L);

        for (int i = 0; i < 4; i++) {
            assertThat(window.tryAcquire(rule, 900L).allowed()).isTrue();
        }

        // Just past the boundary the previous window is still weighted at ~99%, so the caller gets a
        // sliver of allowance rather than a whole fresh window - a naive reset would grant all 4 again.
        int allowedRightAfterBoundary = 0;
        for (long t = 1_010L; t < 1_020L; t++) {
            if (window.tryAcquire(rule, t).allowed()) {
                allowedRightAfterBoundary++;
            }
        }
        assertThat(allowedRightAfterBoundary).isEqualTo(1);
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
