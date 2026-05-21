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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class GuideChatUsageService {

    private static final Logger log = LoggerFactory.getLogger(GuideChatUsageService.class);
    private static final String DAILY_KEY_PREFIX = "ai:guide:daily:";
    private static final String WINDOW_KEY_PREFIX = "ai:guide:window:";
    private static final int MAX_TRACKED_CLIENTS = 10_000;

    private final RedisTemplate<String, Object> redisTemplate;
    private final Map<String, UsageBucket> fallbackUsage = new ConcurrentHashMap<>();

    @Value("${app.security.ai-guide.daily-limit:${AI_GUIDE_DAILY_LIMIT_PER_CLIENT:20}}")
    private int dailyLimit;

    @Value("${app.security.ai-guide.window-limit:${AI_GUIDE_WINDOW_LIMIT_PER_CLIENT:8}}")
    private int windowLimit;

    @Value("${app.security.ai-guide.window-seconds:${AI_GUIDE_WINDOW_SECONDS:600}}")
    private int windowSeconds;

    public GuideChatUsageService(ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    public Decision tryAcquire(String clientKey) {
        String identity = shortHash(clientKey == null || clientKey.isBlank() ? "unknown" : clientKey);
        String dateKey = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);

        if (redisTemplate != null) {
            try {
                Decision redisDecision = tryAcquireWithRedis(identity, dateKey);
                if (redisDecision != null) {
                    return redisDecision;
                }
            } catch (Exception e) {
                log.warn("Redis guide chat usage check failed, using in-memory fallback: {}", e.getMessage());
            }
        }

        return tryAcquireInMemory(identity, dateKey);
    }

    private Decision tryAcquireWithRedis(String identity, String dateKey) {
        int safeDailyLimit = Math.max(1, dailyLimit);
        int safeWindowLimit = Math.max(1, windowLimit);
        int safeWindowSeconds = Math.max(30, windowSeconds);

        String dailyKey = DAILY_KEY_PREFIX + dateKey + ":" + identity;
        String windowKey = WINDOW_KEY_PREFIX + identity;

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

    private Decision tryAcquireInMemory(String identity, String dateKey) {
        cleanupIfNeeded();
        UsageBucket bucket = fallbackUsage.computeIfAbsent(identity, ignored -> new UsageBucket(dateKey));
        return bucket.tryAcquire(dateKey, Math.max(1, dailyLimit), Math.max(1, windowLimit), Math.max(30, windowSeconds));
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
            byte[] hashed = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed, 0, 12);
        } catch (Exception e) {
            return "unknown";
        }
    }

    public record Decision(boolean allowed, String reason, int remaining, int retryAfterSeconds) {
        static Decision allowed(int remaining, int retryAfterSeconds) {
            return new Decision(true, "", remaining, retryAfterSeconds);
        }

        static Decision blocked(String reason, int remaining, int retryAfterSeconds) {
            return new Decision(false, reason, remaining, Math.max(1, retryAfterSeconds));
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

        synchronized Decision tryAcquire(String currentDateKey, int dailyLimit, int windowLimit, int windowSeconds) {
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
