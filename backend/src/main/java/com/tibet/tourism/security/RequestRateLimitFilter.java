package com.tibet.tourism.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RequestRateLimitFilter extends OncePerRequestFilter {

    private static final int MAX_TRACKED_WINDOWS = 20_000;

    private final Map<String, RateWindow> windows = new ConcurrentHashMap<>();

    @Value("${app.security.rate-limit.enabled:true}")
    private boolean enabled;

    @Value("${app.security.rate-limit.default.requests:300}")
    private int defaultRequests;

    @Value("${app.security.rate-limit.default.window-seconds:60}")
    private long defaultWindowSeconds;

    @Value("${app.security.rate-limit.auth.requests:12}")
    private int authRequests;

    @Value("${app.security.rate-limit.auth.window-seconds:60}")
    private long authWindowSeconds;

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

        LimitRule rule = resolveRule(path);
        cleanupIfNeeded(System.currentTimeMillis());
        String key = rule.name() + ":" + clientIp(request);
        long now = System.currentTimeMillis();
        RateWindow window = windows.computeIfAbsent(key, ignored -> new RateWindow(now));
        RateDecision decision = window.tryAcquire(rule, now);

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

    private void cleanupIfNeeded(long now) {
        if (windows.size() < MAX_TRACKED_WINDOWS) {
            return;
        }
        windows.entrySet().removeIf(entry -> now - entry.getValue().lastSeenAt() > Duration.ofMinutes(30).toMillis());
    }

    private LimitRule resolveRule(String path) {
        if (path.equals("/api/auth/login") || path.equals("/api/auth/register")) {
            return new LimitRule("auth", authRequests, Duration.ofSeconds(authWindowSeconds).toMillis());
        }
        if (path.equals("/api/routes/generate") || path.equals("/api/routes/generate/stream")) {
            return new LimitRule("ai", aiRequests, Duration.ofSeconds(aiWindowSeconds).toMillis());
        }
        if (path.contains("/upload-image") || path.endsWith("/upload-avatar")) {
            return new LimitRule("upload", uploadRequests, Duration.ofSeconds(uploadWindowSeconds).toMillis());
        }
        if (path.startsWith("/api/admin/")) {
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
        private int count;

        RateWindow(long now) {
            this.windowStartedAt = now;
            this.lastSeenAt = now;
        }

        synchronized RateDecision tryAcquire(LimitRule rule, long now) {
            lastSeenAt = now;
            if (now - windowStartedAt >= rule.windowMillis()) {
                windowStartedAt = now;
                count = 0;
            }

            long resetAt = windowStartedAt + rule.windowMillis();
            long retryAfter = Math.max(1, (resetAt - now + 999) / 1000);

            if (count >= rule.maxRequests()) {
                return new RateDecision(false, 0, retryAfter, resetAt / 1000);
            }

            count++;
            int remaining = Math.max(0, rule.maxRequests() - count);
            return new RateDecision(true, remaining, retryAfter, resetAt / 1000);
        }

        synchronized long lastSeenAt() {
            return lastSeenAt;
        }
    }
}
