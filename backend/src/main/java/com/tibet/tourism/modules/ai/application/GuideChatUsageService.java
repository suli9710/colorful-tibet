package com.tibet.tourism.modules.ai.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class GuideChatUsageService {

    private static final Logger log = LoggerFactory.getLogger(GuideChatUsageService.class);
    private static final String DAILY_KEY_PREFIX = "ai:guide:daily:";
    private static final String WINDOW_KEY_PREFIX = "ai:guide:window:";
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final StringRedisTemplate redisTemplate;
    private final Map<String, UsageBucket> fallbackUsage = new ConcurrentHashMap<>();

    @Value("${app.security.guide-chat.anonymous-enabled:${GUIDE_CHAT_ANONYMOUS_ENABLED:true}}")
    private boolean anonymousEnabled;

    @Value("${app.security.guide-chat.anonymous-daily-quota-per-ip:${GUIDE_CHAT_ANONYMOUS_DAILY_QUOTA_PER_IP:5}}")
    private int anonymousDailyLimit;

    @Value("${app.security.guide-chat.authenticated-daily-quota-per-user:${GUIDE_CHAT_AUTHENTICATED_DAILY_QUOTA_PER_USER:30}}")
    private int authenticatedDailyLimit;

    @Value("${app.security.guide-chat.require-recaptcha-after:${GUIDE_CHAT_REQUIRE_RECAPTCHA_AFTER:2}}")
    private int requireRecaptchaAfter;

    @Value("${app.security.guide-chat.window-limit:${app.security.ai-guide.window-limit:${AI_GUIDE_WINDOW_LIMIT_PER_CLIENT:8}}}")
    private int windowLimit;

    @Value("${app.security.guide-chat.window-seconds:${app.security.ai-guide.window-seconds:${AI_GUIDE_WINDOW_SECONDS:600}}}")
    private int windowSeconds;

    public GuideChatUsageService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    public Decision tryAcquire(String clientKey) {
        return tryAcquire(ClientIdentity.anonymous(clientKey), false);
    }

    public Decision tryAcquire(ClientIdentity client, boolean recaptchaVerified) {
        ClientIdentity safeClient = client == null ? ClientIdentity.anonymous("unknown") : client;
        if (!safeClient.authenticated() && !anonymousEnabled) {
            return Decision.blocked("anonymous-disabled", 0, 3600);
        }

        String identity = safeClient.redisScope() + ":" + shortHash(safeClient.key());
        String dateKey = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        if (redisTemplate != null) {
            try {
                Decision redisDecision = tryAcquireWithRedis(identity, dateKey, safeClient.authenticated(), recaptchaVerified);
                if (redisDecision != null) {
                    return redisDecision;
                }
            } catch (Exception e) {
                log.warn("Redis guide chat usage check failed, using in-memory fallback: {}", e.getMessage());
            }
        }

        return tryAcquireInMemory(identity, dateKey, safeClient.authenticated(), recaptchaVerified);
    }

    private Decision tryAcquireWithRedis(String identity, String dateKey, boolean authenticated, boolean recaptchaVerified) {
        int safeDailyLimit = dailyLimitFor(authenticated);
        int safeWindowLimit = Math.max(1, windowLimit);
        int safeWindowSeconds = Math.max(30, windowSeconds);

        String dailyKey = DAILY_KEY_PREFIX + dateKey + ":" + identity;
        String windowKey = WINDOW_KEY_PREFIX + identity;

        int currentDaily = parseCount(redisTemplate.opsForValue().get(dailyKey));
        if (currentDaily >= safeDailyLimit) {
            return Decision.blocked("daily", 0, secondsUntilTomorrow());
        }
        if (requiresRecaptcha(authenticated, recaptchaVerified, currentDaily)) {
            return Decision.challenge("captcha", Math.max(1, safeWindowSeconds));
        }

        Long dailyCount = redisTemplate.opsForValue().increment(dailyKey);
        if (dailyCount != null && dailyCount == 1L) {
            redisTemplate.expire(dailyKey, Duration.ofSeconds(secondsUntilTomorrow() + 3600));
        }

        Long windowCount = redisTemplate.opsForValue().increment(windowKey);
        if (windowCount != null && windowCount == 1L) {
            redisTemplate.expire(windowKey, Duration.ofSeconds(safeWindowSeconds));
        }

        if (dailyCount == null || windowCount == null) {
            return null;
        }

        if (dailyCount > safeDailyLimit) {
            return Decision.blocked("daily", 0, secondsUntilTomorrow());
        }

        Long ttlSeconds = redisTemplate.getExpire(windowKey, TimeUnit.SECONDS);
        int retryAfter = ttlSeconds == null || ttlSeconds <= 0 ? safeWindowSeconds : Math.toIntExact(ttlSeconds);
        if (windowCount > safeWindowLimit) {
            return Decision.blocked("window", 0, retryAfter);
        }

        int remaining = Math.min(
                Math.max(0, safeDailyLimit - Math.toIntExact(dailyCount)),
                Math.max(0, safeWindowLimit - Math.toIntExact(windowCount)));
        return Decision.allowed(remaining, retryAfter);
    }

    private Decision tryAcquireInMemory(String identity, String dateKey, boolean authenticated, boolean recaptchaVerified) {
        cleanupIfNeeded();
        UsageBucket bucket = fallbackUsage.computeIfAbsent(identity, ignored -> new UsageBucket(dateKey));
        return bucket.tryAcquire(
                dateKey,
                dailyLimitFor(authenticated),
                Math.max(1, windowLimit),
                Math.max(30, windowSeconds),
                authenticated,
                Math.max(0, requireRecaptchaAfter),
                recaptchaVerified);
    }

    private int dailyLimitFor(boolean authenticated) {
        return Math.max(1, authenticated ? authenticatedDailyLimit : anonymousDailyLimit);
    }

    private boolean requiresRecaptcha(boolean authenticated, boolean recaptchaVerified, int currentDaily) {
        return !authenticated
                && !recaptchaVerified
                && requireRecaptchaAfter >= 0
                && currentDaily >= Math.max(0, requireRecaptchaAfter);
    }

    private int parseCount(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(value));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private void cleanupIfNeeded() {
        if (fallbackUsage.size() <= MAX_TRACKED_CLIENTS) {
            return;
        }
        long cutoff = System.currentTimeMillis() - Duration.ofHours(2).toMillis();
        fallbackUsage.entrySet().removeIf(entry -> entry.getValue().lastSeenAt() < cutoff);
    }

    private int secondsUntilTomorrow() {
        long now = System.currentTimeMillis();
        long tomorrow = LocalDate.now()
                .plusDays(1)
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        return Math.max(60, (int) ((tomorrow - now + 999) / 1000));
    }

    private String shortHash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed, 0, 12);
        } catch (Exception e) {
            return "unknown";
        }
    }

    public record ClientIdentity(boolean authenticated, String key) {
        public static ClientIdentity anonymous(String key) {
            return new ClientIdentity(false, key);
        }

        public static ClientIdentity authenticated(Long userId) {
            return new ClientIdentity(true, userId == null ? "unknown" : userId.toString());
        }

        String redisScope() {
            return authenticated ? "user" : "anon";
        }
    }

    public record Decision(boolean allowed, boolean challengeRequired, String reason, int remaining, int retryAfterSeconds) {
        static Decision allowed(int remaining, int retryAfterSeconds) {
            return new Decision(true, false, "", remaining, retryAfterSeconds);
        }

        static Decision blocked(String reason, int remaining, int retryAfterSeconds) {
            return new Decision(false, false, reason, remaining, Math.max(1, retryAfterSeconds));
        }

        static Decision challenge(String reason, int retryAfterSeconds) {
            return new Decision(false, true, reason, 0, Math.max(1, retryAfterSeconds));
        }
    }

    private static final class UsageBucket {
        private String dateKey;
        private int dailyCount;
        private int windowCount;
        private long windowStartedAt;
        private long lastSeenAt;

        private UsageBucket(String dateKey) {
            long now = System.currentTimeMillis();
            this.dateKey = dateKey;
            this.windowStartedAt = now;
            this.lastSeenAt = now;
        }

        synchronized Decision tryAcquire(String currentDateKey, int dailyLimit, int windowLimit, int windowSeconds,
                                         boolean authenticated, int requireRecaptchaAfter, boolean recaptchaVerified) {
            long now = System.currentTimeMillis();
            lastSeenAt = now;

            if (!currentDateKey.equals(dateKey)) {
                dateKey = currentDateKey;
                dailyCount = 0;
                windowCount = 0;
                windowStartedAt = now;
            }

            long windowMillis = Duration.ofSeconds(windowSeconds).toMillis();
            if (now - windowStartedAt >= windowMillis) {
                windowStartedAt = now;
                windowCount = 0;
            }

            int retryAfter = Math.max(1, (int) ((windowStartedAt + windowMillis - now + 999) / 1000));
            if (dailyCount >= dailyLimit) {
                return Decision.blocked("daily", 0, retryAfter);
            }
            if (!authenticated && !recaptchaVerified && dailyCount >= requireRecaptchaAfter) {
                return Decision.challenge("captcha", retryAfter);
            }
            if (windowCount >= windowLimit) {
                return Decision.blocked("window", 0, retryAfter);
            }

            dailyCount++;
            windowCount++;
            int remaining = Math.min(dailyLimit - dailyCount, windowLimit - windowCount);
            return Decision.allowed(Math.max(0, remaining), retryAfter);
        }

        synchronized long lastSeenAt() {
            return lastSeenAt;
        }
    }
}
