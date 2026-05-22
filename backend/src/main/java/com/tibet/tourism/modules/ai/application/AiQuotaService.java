package com.tibet.tourism.modules.ai.application;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class AiQuotaService {

    private static final Logger log = LoggerFactory.getLogger(AiQuotaService.class);
    private static final String QUOTA_KEY_PREFIX = "ai:quota:daily:";
    private static final String CACHE_KEY_PREFIX = "ai:cache:route:";

    private final StringRedisTemplate redisTemplate;

    @Value("${app.security.ai-quota.daily-limit:${AI_DAILY_QUOTA_PER_USER:20}}")
    private int dailyLimit;

    @Value("${app.security.ai-quota.cache-ttl-seconds:${AI_CACHE_TTL_SECONDS:300}}")
    private int cacheTtlSeconds;

    private final ConcurrentHashMap<String, AtomicInteger> fallbackQuota = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CacheEntry> fallbackCache = new ConcurrentHashMap<>();
    private volatile String fallbackDateKey = "";

    public AiQuotaService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    public boolean isQuotaExceeded(Long userId) {
        if (userId == null) {
            return false;
        }
        String dateKey = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String key = QUOTA_KEY_PREFIX + dateKey + ":" + userId;

        if (redisTemplate != null) {
            try {
                String val = redisTemplate.opsForValue().get(key);
                int count = parseInt(val);
                return count >= dailyLimit;
            } catch (Exception e) {
                log.warn("Redis quota check failed, using in-memory fallback: {}", e.getMessage());
                return isFallbackQuotaExceeded(dateKey, userId);
            }
        }
        return isFallbackQuotaExceeded(dateKey, userId);
    }

    public void incrementQuota(Long userId) {
        if (userId == null) {
            return;
        }
        String dateKey = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String key = QUOTA_KEY_PREFIX + dateKey + ":" + userId;
        incrementFallbackQuota(dateKey, userId);

        if (redisTemplate != null) {
            try {
                Long count = redisTemplate.opsForValue().increment(key);
                if (count != null && count == 1L) {
                    redisTemplate.expire(key, Duration.ofHours(25));
                }
                return;
            } catch (Exception e) {
                log.warn("Redis quota increment failed: {}", e.getMessage());
            }
        }
    }

    public int getRemainingQuota(Long userId) {
        if (userId == null) {
            return dailyLimit;
        }
        String dateKey = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String key = QUOTA_KEY_PREFIX + dateKey + ":" + userId;

        int used = 0;
        if (redisTemplate != null) {
            try {
                String val = redisTemplate.opsForValue().get(key);
                used = parseInt(val);
            } catch (Exception e) {
                log.warn("Redis quota remaining check failed, using in-memory fallback: {}", e.getMessage());
                used = getFallbackQuotaCount(dateKey, userId);
            }
        } else {
            used = getFallbackQuotaCount(dateKey, userId);
        }
        return Math.max(0, dailyLimit - used);
    }

    public String getCachedRoute(String cacheKey) {
        if (redisTemplate != null) {
            try {
                String val = redisTemplate.opsForValue().get(CACHE_KEY_PREFIX + cacheKey);
                return val == null ? null : val.toString();
            } catch (Exception e) {
                log.warn("Redis cache read failed: {}", e.getMessage());
            }
        }
        CacheEntry entry = fallbackCache.get(cacheKey);
        if (entry != null && !entry.isExpired()) {
            return entry.value;
        }
        fallbackCache.remove(cacheKey);
        return null;
    }

    public void cacheRoute(String cacheKey, String content) {
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(
                        CACHE_KEY_PREFIX + cacheKey, content, Duration.ofSeconds(cacheTtlSeconds));
                return;
            } catch (Exception e) {
                log.warn("Redis cache write failed: {}", e.getMessage());
            }
        }
        fallbackCache.put(cacheKey, new CacheEntry(content, System.currentTimeMillis() + cacheTtlSeconds * 1000L));
        if (fallbackCache.size() > 100) {
            fallbackCache.entrySet().removeIf(e -> e.getValue().isExpired());
        }
    }

    public String buildCacheKey(Long userId, int days, String budget, String preference, String locale) {
        return userId + ":" + days + ":" + normalize(budget) + ":" + normalize(preference) + ":" + normalize(locale);
    }

    private static int parseInt(Object val) {
        if (val == null) {
            return 0;
        }
        if (val instanceof Number n) {
            return n.intValue();
        }
        try {
            return Integer.parseInt(val.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private String normalize(String val) {
        return val == null ? "" : val.trim().toLowerCase();
    }

    private boolean isFallbackQuotaExceeded(String dateKey, Long userId) {
        return getFallbackQuotaCount(dateKey, userId) >= dailyLimit;
    }

    private int getFallbackQuotaCount(String dateKey, Long userId) {
        resetFallbackIfNewDay(dateKey);
        AtomicInteger counter = fallbackQuota.computeIfAbsent(userId.toString(), k -> new AtomicInteger(0));
        return counter.get();
    }

    private void incrementFallbackQuota(String dateKey, Long userId) {
        resetFallbackIfNewDay(dateKey);
        fallbackQuota.computeIfAbsent(userId.toString(), k -> new AtomicInteger(0)).incrementAndGet();
    }

    private void resetFallbackIfNewDay(String dateKey) {
        if (!dateKey.equals(fallbackDateKey)) {
            synchronized (this) {
                if (!dateKey.equals(fallbackDateKey)) {
                    fallbackQuota.clear();
                    fallbackCache.entrySet().removeIf(e -> e.getValue().isExpired());
                    fallbackDateKey = dateKey;
                }
            }
        }
    }

    private static class CacheEntry {
        final String value;
        final long expiresAt;

        CacheEntry(String value, long expiresAt) {
            this.value = value;
            this.expiresAt = expiresAt;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
