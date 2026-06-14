package com.tibet.tourism.common.security;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.annotation.Order;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RequestRateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestRateLimitFilter.class);
    private static final int MAX_TRACKED_WINDOWS = 20_000;
    private static final long CLEANUP_INTERVAL_MILLIS = Duration.ofMinutes(1).toMillis();
    private static final long MIN_RATE_LIMIT_WINDOW_MILLIS = 1_000L;
    private static final long MAX_RATE_LIMIT_WINDOW_MILLIS = Duration.ofDays(1).toMillis();
    static final String INCREMENT_WITH_EXPIRE_LUA =
            """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 or redis.call('TTL', KEYS[1]) < 0 then
              redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """;
    private static final RedisScript<Long> INCREMENT_WITH_EXPIRE_SCRIPT = RedisScript.of(
            INCREMENT_WITH_EXPIRE_LUA,
            Long.class);

    private final Map<String, RateWindow> windows = new ConcurrentHashMap<>();
    private final StringRedisTemplate redisTemplate;
    private final TrustedProxyIpResolver trustedProxyIpResolver;
    private final AtomicLong lastCleanupAt = new AtomicLong(0);
    private final AtomicLong redisFallbackActive = new AtomicLong(0);
    private final AtomicLong redisFallbackEvents = new AtomicLong(0);
    private final Counter redisFallbackCounter;

    public RequestRateLimitFilter(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                                  TrustedProxyIpResolver trustedProxyIpResolver,
                                  ObjectProvider<MeterRegistry> meterRegistryProvider) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.trustedProxyIpResolver = trustedProxyIpResolver;
        MeterRegistry meterRegistry = meterRegistryProvider.getIfAvailable();
        if (meterRegistry == null) {
            this.redisFallbackCounter = null;
        } else {
            this.redisFallbackCounter = Counter.builder("app.security.rate.limit.redis.fallback.events")
                    .description("Number of rate limit requests served by in-memory fallback after Redis failure")
                    .register(meterRegistry);
            Gauge.builder("app.security.rate.limit.redis.fallback.active", redisFallbackActive, AtomicLong::get)
                    .description("Whether rate limiting is currently using in-memory fallback because Redis failed")
                    .register(meterRegistry);
        }
    }

    @Value("${app.security.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.security.rate-limit.redis-enabled:true}")
    private boolean redisEnabled;

    @Value("${app.security.rate-limit.redis-fail-closed:false}")
    private boolean redisFailClosed;

    @Value("${app.security.rate-limit.default.requests:300}")
    private int defaultRequests;

    @Value("${app.security.rate-limit.default.window-seconds:60}")
    private long defaultWindowSeconds;

    @Value("${app.security.rate-limit.auth.requests:12}")
    private int authRequests;

    @Value("${app.security.rate-limit.auth.window-seconds:60}")
    private long authWindowSeconds;

    @Value("${app.security.rate-limit.register.requests:5}")
    private int registerRequests;

    @Value("${app.security.rate-limit.register.window-seconds:3600}")
    private long registerWindowSeconds;

    @Value("${app.security.rate-limit.ai.requests:6}")
    private int aiRequests;

    @Value("${app.security.rate-limit.ai.window-seconds:600}")
    private long aiWindowSeconds;

    @Value("${app.security.rate-limit.guide-chat.requests:${app.security.guide-chat.window-limit:8}}")
    private int guideChatRequests;

    @Value("${app.security.rate-limit.guide-chat.window-seconds:${app.security.guide-chat.window-seconds:300}}")
    private long guideChatWindowSeconds;

    @Value("${app.security.rate-limit.upload.requests:30}")
    private int uploadRequests;

    @Value("${app.security.rate-limit.upload.window-seconds:60}")
    private long uploadWindowSeconds;

    @Value("${app.security.rate-limit.admin.requests:120}")
    private int adminRequests;

    @Value("${app.security.rate-limit.admin.window-seconds:60}")
    private long adminWindowSeconds;

    @Value("${app.security.rate-limit.trust-proxy-headers:false}")
    private boolean trustProxyHeaders;

    @Value("${app.security.rate-limit.trusted-proxy-cidrs:${app.security.trusted-proxy-cidrs:}}")
    private String trustedProxyCidrs;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if (!enabled || !path.startsWith("/api/") || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = System.currentTimeMillis();
        LimitRule rule = resolveRule(path, request.getMethod());
        RateDecision decision = tryAcquire(request, rule, now);

        response.setHeader("X-RateLimit-Limit", String.valueOf(rule.maxRequests()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(decision.resetEpochSeconds()));

        if (!decision.allowed()) {
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
            SecurityErrorResponseWriter.writeJson(
                    response,
                    decision.status(),
                    decision.error(),
                    decision.message());
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateDecision tryAcquire(HttpServletRequest request, LimitRule rule, long now) {
        if (redisEnabled) {
            if (redisTemplate == null) {
                recordRedisFallback("missing-template", "Redis rate limit backend is not configured");
                if (shouldFailClosed(rule)) {
                    return backendUnavailable(now);
                }
            } else {
                try {
                    RateDecision redisDecision = tryAcquireWithRedis(request, rule, now);
                    if (redisDecision != null) {
                        clearRedisFallbackIfActive();
                        return redisDecision;
                    }
                    recordRedisFallback("empty-result", "Redis script returned no count");
                    if (shouldFailClosed(rule)) {
                        return backendUnavailable(now);
                    }
                } catch (Exception e) {
                    recordRedisFallback("exception", SensitiveLogSanitizer.exceptionSummary(e));
                    if (shouldFailClosed(rule)) {
                        return backendUnavailable(now);
                    }
                }
            }
        }

        return tryAcquireInMemory(request, rule, now);
    }

    private boolean shouldFailClosed(LimitRule rule) {
        return redisFailClosed && redisEnabled && rule.sensitive();
    }

    private RateDecision backendUnavailable(long now) {
        long retryAfterSeconds = 60;
        return new RateDecision(
                false,
                0,
                retryAfterSeconds,
                (now / 1000) + retryAfterSeconds,
                503,
                "Service Unavailable",
                "Rate limit verification is temporarily unavailable");
    }

    private void recordRedisFallback(String reason, String detail) {
        redisFallbackEvents.incrementAndGet();
        if (redisFallbackCounter != null) {
            redisFallbackCounter.increment();
        }
        String fallbackDetail = detail == null || detail.isBlank() ? "unavailable" : detail;
        if (redisFallbackActive.compareAndSet(0, 1)) {
            logger.warn("Redis rate limiting unavailable; using in-memory fallback counters. reason={}, detail={}",
                    reason,
                    fallbackDetail);
            return;
        }
        logger.debug("Redis rate limiting still unavailable; continuing in-memory fallback. reason={}, detail={}",
                reason,
                fallbackDetail);
    }

    private void clearRedisFallbackIfActive() {
        if (redisFallbackActive.compareAndSet(1, 0)) {
            logger.info("Redis rate limiting recovered; using Redis counters again");
        }
    }

    boolean isRedisFallbackActive() {
        return redisFallbackActive.get() == 1;
    }

    long redisFallbackEvents() {
        return redisFallbackEvents.get();
    }

    private RateDecision tryAcquireWithRedis(HttpServletRequest request, LimitRule rule, long now) {
        String key = redisKey(request, rule);
        long windowSeconds = Math.max(1, rule.windowMillis() / 1000);
        Long count = redisTemplate.execute(INCREMENT_WITH_EXPIRE_SCRIPT, List.of(key), String.valueOf(windowSeconds));
        if (count == null) {
            return null;
        }

        Long ttlSeconds = redisTemplate.getExpire(key, TimeUnit.SECONDS);
        long retryAfter = ttlSeconds == null || ttlSeconds <= 0 ? windowSeconds : ttlSeconds;
        long resetAt = now + retryAfter * 1000;
        int remaining = (int) Math.max(0, rule.maxRequests() - count);
        return new RateDecision(count <= rule.maxRequests(), remaining, retryAfter, resetAt / 1000);
    }

    private RateDecision tryAcquireInMemory(HttpServletRequest request, LimitRule rule, long now) {
        cleanupIfNeeded(now);
        String key = rule.name() + ":" + clientIdentity(request);
        RateWindow window = windows.computeIfAbsent(key, ignored -> new RateWindow(now));
        return window.tryAcquire(rule, now);
    }

    private void cleanupIfNeeded(long now) {
        if (windows.size() < MAX_TRACKED_WINDOWS) {
            return;
        }
        long lastCleanup = lastCleanupAt.get();
        if (now - lastCleanup < CLEANUP_INTERVAL_MILLIS || !lastCleanupAt.compareAndSet(lastCleanup, now)) {
            return;
        }
        windows.entrySet().removeIf(entry -> now - entry.getValue().lastSeenAt() > Duration.ofMinutes(30).toMillis());
    }

    private LimitRule resolveRule(String path, String method) {
        String normalized = path.toLowerCase();
        if (normalized.startsWith("/api/auth/register")) {
            return new LimitRule("register", registerRequests, configuredWindowMillis(registerWindowSeconds), true);
        }
        if (normalized.startsWith("/api/auth/login")) {
            return new LimitRule("auth", authRequests, configuredWindowMillis(authWindowSeconds), true);
        }
        if ("GET".equalsIgnoreCase(method) && normalized.startsWith("/api/routes/generate/jobs/")) {
            return new LimitRule("default", defaultRequests, configuredWindowMillis(defaultWindowSeconds), false);
        }
        if (normalized.startsWith("/api/routes/generate")) {
            return new LimitRule("ai", aiRequests, configuredWindowMillis(aiWindowSeconds), true);
        }
        if (normalized.startsWith("/api/guide/chat")) {
            return new LimitRule("guide-chat", guideChatRequests, configuredWindowMillis(guideChatWindowSeconds), true);
        }
        if (normalized.contains("/upload-image") || normalized.endsWith("/upload-avatar")) {
            return new LimitRule("upload", uploadRequests, configuredWindowMillis(uploadWindowSeconds), true);
        }
        if (normalized.startsWith("/api/admin/")) {
            return new LimitRule("admin", adminRequests, configuredWindowMillis(adminWindowSeconds), true);
        }
        if (isStateChangingMethod(method)) {
            return new LimitRule("mutation", defaultRequests, configuredWindowMillis(defaultWindowSeconds), true);
        }
        return new LimitRule("default", defaultRequests, configuredWindowMillis(defaultWindowSeconds), false);
    }

    private boolean isStateChangingMethod(String method) {
        return "POST".equalsIgnoreCase(method)
                || "PUT".equalsIgnoreCase(method)
                || "PATCH".equalsIgnoreCase(method)
                || "DELETE".equalsIgnoreCase(method);
    }

    private long configuredWindowMillis(long windowSeconds) {
        if (windowSeconds <= 0) {
            return MIN_RATE_LIMIT_WINDOW_MILLIS;
        }
        long maxSeconds = TimeUnit.MILLISECONDS.toSeconds(MAX_RATE_LIMIT_WINDOW_MILLIS);
        return TimeUnit.SECONDS.toMillis(Math.min(windowSeconds, maxSeconds));
    }

    private String clientIp(HttpServletRequest request) {
        return trustedProxyIpResolver.resolveClientIp(request, trustProxyHeaders, trustedProxyCidrs);
    }

    private String redisKey(HttpServletRequest request, LimitRule rule) {
        return "rate-limit:" + rule.name() + ":" + clientIdentity(request);
    }

    private String clientIdentity(HttpServletRequest request) {
        return "ip:" + shortHash(clientIp(request));
    }

    private String shortHash(String value) {
        String normalized = value == null ? "" : value;
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed, 0, 8);
        } catch (Exception e) {
            return "unknown";
        }
    }

    private record LimitRule(String name, int maxRequests, long windowMillis, boolean sensitive) {
        private static final int MIN_REQUESTS = 1;

        private LimitRule {
            maxRequests = Math.max(MIN_REQUESTS, maxRequests);
            windowMillis = Math.min(
                    MAX_RATE_LIMIT_WINDOW_MILLIS,
                    Math.max(MIN_RATE_LIMIT_WINDOW_MILLIS, windowMillis));
        }
    }

    private record RateDecision(
            boolean allowed,
            int remaining,
            long retryAfterSeconds,
            long resetEpochSeconds,
            int status,
            String error,
            String message) {
        private RateDecision(boolean allowed, int remaining, long retryAfterSeconds, long resetEpochSeconds) {
            this(
                    allowed,
                    remaining,
                    retryAfterSeconds,
                    resetEpochSeconds,
                    429,
                    "Too Many Requests",
                    "Request rate limit exceeded");
        }
    }

    private static class RateWindow {
        private long windowStartedAt;
        private long lastSeenAt;
        private int currentCount;
        private int prevCount;

        RateWindow(long now) {
            this.windowStartedAt = now;
            this.lastSeenAt = now;
        }

        synchronized RateDecision tryAcquire(LimitRule rule, long now) {
            lastSeenAt = now;
            long windowMs = rule.windowMillis();

            if (now - windowStartedAt >= windowMs) {
                if (now - windowStartedAt >= windowMs * 2) {
                    prevCount = 0;
                } else {
                    prevCount = currentCount;
                }
                windowStartedAt += windowMs;
                currentCount = 0;
            }

            long elapsedInWindow = now - windowStartedAt;
            double prevWeight = prevCount * Math.max(0.0, 1.0 - (double) elapsedInWindow / windowMs);
            double weightedTotal = prevWeight + currentCount;

            long resetAt = windowStartedAt + windowMs;
            long retryAfterSeconds = Math.max(1, (resetAt - now + 999) / 1000);

            if (weightedTotal >= rule.maxRequests()) {
                int remaining = (int) Math.max(0, rule.maxRequests() - Math.ceil(weightedTotal));
                return new RateDecision(false, remaining, retryAfterSeconds, resetAt / 1000);
            }

            currentCount++;
            int remaining = (int) Math.max(0, rule.maxRequests() - Math.ceil(weightedTotal + 1));
            return new RateDecision(true, remaining, retryAfterSeconds, resetAt / 1000);
        }

        synchronized long lastSeenAt() {
            return lastSeenAt;
        }
    }
}
