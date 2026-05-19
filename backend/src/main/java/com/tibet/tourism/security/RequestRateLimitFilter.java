package com.tibet.tourism.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.List;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RequestRateLimitFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestRateLimitFilter.class);
    private static final int MAX_TRACKED_WINDOWS = 20_000;
    private static final long CLEANUP_INTERVAL_MILLIS = Duration.ofMinutes(1).toMillis();
    private static final RedisScript<Long> INCREMENT_WITH_EXPIRE_SCRIPT = RedisScript.of(
            """
            local current = redis.call('INCR', KEYS[1])
            if current == 1 then
              redis.call('EXPIRE', KEYS[1], ARGV[1])
            end
            return current
            """,
            Long.class);

    private final Map<String, RateWindow> windows = new ConcurrentHashMap<>();
    private final RedisTemplate<String, Object> redisTemplate;
    private final AtomicLong lastCleanupAt = new AtomicLong(0);

    public RequestRateLimitFilter(ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    @Value("${app.security.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.security.rate-limit.redis-enabled:true}")
    private boolean redisEnabled;

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

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getServletPath();
        if (!enabled || !path.startsWith("/api/") || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        long now = System.currentTimeMillis();
        LimitRule rule = resolveRule(path);
        RateDecision decision = tryAcquire(request, rule, now);

        response.setHeader("X-RateLimit-Limit", String.valueOf(rule.maxRequests()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
        response.setHeader("X-RateLimit-Reset", String.valueOf(decision.resetEpochSeconds()));

        if (!decision.allowed()) {
            response.setStatus(429);
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(decision.retryAfterSeconds()));
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.getWriter().write("{\"error\":\"Too Many Requests\",\"message\":\"请求过于频繁，请稍后再试\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private RateDecision tryAcquire(HttpServletRequest request, LimitRule rule, long now) {
        if (redisEnabled && redisTemplate != null) {
            try {
                RateDecision redisDecision = tryAcquireWithRedis(request, rule, now);
                if (redisDecision != null) {
                    return redisDecision;
                }
            } catch (Exception e) {
                logger.debug("Redis rate limiting unavailable; falling back to in-memory counters: {}", e.getMessage());
            }
        }

        return tryAcquireInMemory(request, rule, now);
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

    private LimitRule resolveRule(String path) {
        String normalized = path.toLowerCase();
        if (normalized.startsWith("/api/auth/register")) {
            return new LimitRule("register", registerRequests, Duration.ofSeconds(registerWindowSeconds).toMillis());
        }
        if (normalized.startsWith("/api/auth/login")) {
            return new LimitRule("auth", authRequests, Duration.ofSeconds(authWindowSeconds).toMillis());
        }
        if (normalized.startsWith("/api/routes/generate")) {
            return new LimitRule("ai", aiRequests, Duration.ofSeconds(aiWindowSeconds).toMillis());
        }
        if (normalized.contains("/upload-image") || normalized.endsWith("/upload-avatar")) {
            return new LimitRule("upload", uploadRequests, Duration.ofSeconds(uploadWindowSeconds).toMillis());
        }
        if (normalized.startsWith("/api/admin/")) {
            return new LimitRule("admin", adminRequests, Duration.ofSeconds(adminWindowSeconds).toMillis());
        }
        return new LimitRule("default", defaultRequests, Duration.ofSeconds(defaultWindowSeconds).toMillis());
    }

    private String clientIp(HttpServletRequest request) {
        if (trustProxyHeaders) {
            String forwardedFor = request.getHeader("X-Forwarded-For");
            if (hasUsableIp(forwardedFor)) {
                return forwardedFor.split(",")[0].trim();
            }

            String realIp = request.getHeader("X-Real-IP");
            if (hasUsableIp(realIp)) {
                return realIp.trim();
            }
        }

        String remoteAddr = request.getRemoteAddr();
        return remoteAddr == null || remoteAddr.isBlank() ? "unknown" : remoteAddr;
    }

    private String redisKey(HttpServletRequest request, LimitRule rule) {
        return "rate-limit:" + rule.name() + ":" + clientIdentity(request);
    }

    private String clientIdentity(HttpServletRequest request) {
        String authHash = authCookieHash(request);
        String authPart = "anonymous".equals(authHash)
                ? "anon:" + anonymousFingerprint(request)
                : "auth:" + authHash;
        return "ip:" + shortHash(clientIp(request))
                + ":" + authPart;
    }

    private String authCookieHash(HttpServletRequest request) {
        var authCookie = WebUtils.getCookie(request, CookieAuthConstants.AUTH_COOKIE_NAME);
        if (authCookie == null || authCookie.getValue() == null || authCookie.getValue().isBlank()) {
            return "anonymous";
        }
        return shortHash(authCookie.getValue());
    }

    private String anonymousFingerprint(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String acceptLanguage = request.getHeader("Accept-Language");
        return shortHash((userAgent == null ? "" : userAgent)
                + "|"
                + (acceptLanguage == null ? "" : acceptLanguage));
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

    private boolean hasUsableIp(String value) {
        return value != null && !value.isBlank() && value.length() <= 128 && !"unknown".equalsIgnoreCase(value.trim());
    }

    private record LimitRule(String name, int maxRequests, long windowMillis) {
    }

    private record RateDecision(boolean allowed, int remaining, long retryAfterSeconds, long resetEpochSeconds) {
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
