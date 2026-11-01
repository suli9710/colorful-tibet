package com.tibet.tourism.modules.ai.application;
import com.tibet.tourism.common.security.CacheKeyHasher;
import com.tibet.tourism.common.security.ProductionSafetyValidator;
import com.tibet.tourism.common.security.RedisPayloadCipher;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

@Service
public class AiQuotaService {

    private static final Logger log = LoggerFactory.getLogger(AiQuotaService.class);
    private static final String QUOTA_KEY_PREFIX = "ai:quota:daily:";
    private static final String CACHE_KEY_PREFIX = "ai:cache:route:";
    private static final String USER_QUOTA_KEY_NAMESPACE = "ai-quota-user";
    private static final String ROUTE_CACHE_KEY_NAMESPACE = "ai-route";
    private static final long QUOTA_TTL_SECONDS = Duration.ofHours(25).toSeconds();
    private static final DefaultRedisScript<List> CONSUME_QUOTA_SCRIPT = new DefaultRedisScript<>("""
            local current = tonumber(redis.call('GET', KEYS[1]) or '0')
            local limit = tonumber(ARGV[1])
            local ttl = tonumber(ARGV[2])
            if current >= limit then
                return {0, 0}
            end
            current = redis.call('INCR', KEYS[1])
            if current == 1 then
                redis.call('EXPIRE', KEYS[1], ttl)
            end
            return {1, math.max(limit - current, 0)}
            """, List.class);
    private static final DefaultRedisScript<Long> RELEASE_QUOTA_SCRIPT = new DefaultRedisScript<>("""
            local current = tonumber(redis.call('GET', KEYS[1]))
            if not current or current <= 0 then
                return 0
            end
            redis.call('DECR', KEYS[1])
            return 1
            """, Long.class);

    private final StringRedisTemplate redisTemplate;
    private final CacheKeyHasher cacheKeyHasher;
    private final RedisPayloadCipher redisPayloadCipher;
    private final boolean productionSafetyRequired;

    @Value("${app.security.ai-quota.daily-limit:${AI_DAILY_QUOTA_PER_USER:20}}")
    private int dailyLimit;

    @Value("${app.security.ai-quota.cache-ttl-seconds:${AI_CACHE_TTL_SECONDS:300}}")
    private int cacheTtlSeconds;

    private final ConcurrentHashMap<String, AtomicInteger> fallbackQuota = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, CacheEntry> fallbackCache = new ConcurrentHashMap<>();
    private volatile String fallbackDateKey = "";

    @Autowired
    public AiQuotaService(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                          CacheKeyHasher cacheKeyHasher,
                          RedisPayloadCipher redisPayloadCipher,
                          Environment environment) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        this.cacheKeyHasher = cacheKeyHasher;
        this.redisPayloadCipher = redisPayloadCipher;
        this.productionSafetyRequired = ProductionSafetyValidator.isProductionSafetyRequired(environment);
    }

    AiQuotaService(ObjectProvider<StringRedisTemplate> redisTemplateProvider, CacheKeyHasher cacheKeyHasher) {
        this(redisTemplateProvider, cacheKeyHasher, new RedisPayloadCipher("local-cache-key-hmac-secret"), null);
    }

    AiQuotaService(ObjectProvider<StringRedisTemplate> redisTemplateProvider,
                   CacheKeyHasher cacheKeyHasher,
                   Environment environment) {
        this(redisTemplateProvider, cacheKeyHasher, new RedisPayloadCipher("local-cache-key-hmac-secret"), environment);
    }

    public record QuotaConsumptionResult(boolean allowed, int remaining) {}

    public QuotaConsumptionResult tryConsumeQuota(Long userId) {
        if (userId == null) {
            return new QuotaConsumptionResult(true, dailyLimit);
        }
        if (dailyLimit <= 0) {
            return new QuotaConsumptionResult(false, 0);
        }

        String dateKey = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String key = quotaKey(dateKey, userId);

        if (redisTemplate != null) {
            try {
                List<?> result = redisTemplate.execute(
                        CONSUME_QUOTA_SCRIPT,
                        List.of(key),
                        String.valueOf(dailyLimit),
                        String.valueOf(QUOTA_TTL_SECONDS));
                QuotaConsumptionResult decision = toQuotaConsumptionResult(result);
                int used = decision.allowed() ? dailyLimit - decision.remaining() : dailyLimit;
                mirrorFallbackQuota(dateKey, userId, used);
                return decision;
            } catch (Exception e) {
                if (productionSafetyRequired) {
                    log.warn("Redis quota consume failed in production; refusing AI route generation: {}",
                            AiLogPrivacy.exceptionSummary(e));
                    return new QuotaConsumptionResult(false, 0);
                }
                log.warn("Redis quota consume failed, using in-memory fallback: {}", AiLogPrivacy.exceptionSummary(e));
                return tryConsumeFallbackQuota(dateKey, userId);
            }
        }
        if (productionSafetyRequired) {
            log.warn("Redis quota backend is missing in production; refusing AI route generation");
            return new QuotaConsumptionResult(false, 0);
        }
        return tryConsumeFallbackQuota(dateKey, userId);
    }

    public boolean isQuotaExceeded(Long userId) {
        if (userId == null) {
            return false;
        }
        String dateKey = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String key = quotaKey(dateKey, userId);

        if (redisTemplate != null) {
            try {
                String val = redisTemplate.opsForValue().get(key);
                int count = parseInt(val);
                return count >= dailyLimit;
            } catch (Exception e) {
                if (productionSafetyRequired) {
                    log.warn("Redis quota check failed in production; treating quota as exceeded: {}",
                            AiLogPrivacy.exceptionSummary(e));
                    return true;
                }
                log.warn("Redis quota check failed, using in-memory fallback: {}", AiLogPrivacy.exceptionSummary(e));
                return isFallbackQuotaExceeded(dateKey, userId);
            }
        }
        if (productionSafetyRequired) {
            log.warn("Redis quota backend is missing in production; treating quota as exceeded");
            return true;
        }
        return isFallbackQuotaExceeded(dateKey, userId);
    }

    public void incrementQuota(Long userId) {
        tryConsumeQuota(userId);
    }

    /**
     * Hands a consumed unit back when accepted work could not start, such as executor rejection.
     * Best effort: never drops below zero, never resurrects an expired key, and never fails the caller.
     */
    public void releaseQuota(Long userId) {
        if (userId == null || dailyLimit <= 0) {
            return;
        }
        String dateKey = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String key = quotaKey(dateKey, userId);

        if (redisTemplate != null) {
            try {
                // GET + conditional DECR must be one Redis operation. A separate hasKey/DECR pair can
                // race with key expiry and recreate the daily counter at -1 without a TTL.
                redisTemplate.execute(RELEASE_QUOTA_SCRIPT, List.of(key));
            } catch (Exception e) {
                log.warn("Redis quota release failed; in-memory counter still adjusted: {}",
                        AiLogPrivacy.exceptionSummary(e));
            }
        }
        AtomicInteger counter = fallbackQuota.get(key);
        if (counter != null) {
            counter.updateAndGet(used -> Math.max(0, used - 1));
        }
    }

    public int getRemainingQuota(Long userId) {
        if (userId == null) {
            return dailyLimit;
        }
        String dateKey = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String key = quotaKey(dateKey, userId);

        int used = 0;
        if (redisTemplate != null) {
            try {
                String val = redisTemplate.opsForValue().get(key);
                used = parseInt(val);
            } catch (Exception e) {
                if (productionSafetyRequired) {
                    log.warn("Redis quota remaining check failed in production; returning zero remaining: {}",
                            AiLogPrivacy.exceptionSummary(e));
                    return 0;
                }
                log.warn("Redis quota remaining check failed, using in-memory fallback: {}", AiLogPrivacy.exceptionSummary(e));
                used = getFallbackQuotaCount(dateKey, userId);
            }
        } else {
            if (productionSafetyRequired) {
                log.warn("Redis quota backend is missing in production; returning zero remaining");
                return 0;
            }
            used = getFallbackQuotaCount(dateKey, userId);
        }
        return Math.max(0, dailyLimit - used);
    }

    public String getCachedRoute(String cacheKey) {
        if (redisTemplate != null) {
            try {
                String val = redisTemplate.opsForValue().get(CACHE_KEY_PREFIX + cacheKey);
                return val == null ? null : redisPayloadCipher.decrypt(val.toString());
            } catch (Exception e) {
                log.warn("Redis cache read failed: {}", AiLogPrivacy.exceptionSummary(e));
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
                        CACHE_KEY_PREFIX + cacheKey, redisPayloadCipher.encrypt(content), Duration.ofSeconds(cacheTtlSeconds));
                return;
            } catch (Exception e) {
                log.warn("Redis cache write failed: {}", AiLogPrivacy.exceptionSummary(e));
            }
        }
        fallbackCache.put(cacheKey, new CacheEntry(content, System.currentTimeMillis() + cacheTtlSeconds * 1000L));
        if (fallbackCache.size() > 100) {
            fallbackCache.entrySet().removeIf(e -> e.getValue().isExpired());
        }
    }

    public String buildCacheKey(Long userId, int days, String budget, String preference, String locale) {
        String routeFingerprint = String.join(":",
                String.valueOf(userId),
                String.valueOf(days),
                normalize(budget),
                normalize(preference),
                normalize(locale));
        return cacheKeyHasher.cacheKey(ROUTE_CACHE_KEY_NAMESPACE, routeFingerprint);
    }

    private String quotaKey(String dateKey, Long userId) {
        return QUOTA_KEY_PREFIX + dateKey + ":" + cacheKeyHasher.cacheKey(USER_QUOTA_KEY_NAMESPACE, userId);
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

    private QuotaConsumptionResult toQuotaConsumptionResult(List<?> result) {
        if (result == null || result.size() < 2) {
            throw new IllegalStateException("Invalid Redis quota script result");
        }
        boolean allowed = toLong(result.get(0)) == 1L;
        int remaining = Math.max(0, (int) toLong(result.get(1)));
        return new QuotaConsumptionResult(allowed, remaining);
    }

    private static long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return 0L;
        }
        return Long.parseLong(value.toString());
    }

    private String normalize(String val) {
        return val == null ? "" : val.trim().toLowerCase();
    }

    private boolean isFallbackQuotaExceeded(String dateKey, Long userId) {
        return getFallbackQuotaCount(dateKey, userId) >= dailyLimit;
    }

    private int getFallbackQuotaCount(String dateKey, Long userId) {
        resetFallbackIfNewDay(dateKey);
        AtomicInteger counter = fallbackQuota.computeIfAbsent(quotaKey(dateKey, userId), k -> new AtomicInteger(0));
        return counter.get();
    }

    private QuotaConsumptionResult tryConsumeFallbackQuota(String dateKey, Long userId) {
        resetFallbackIfNewDay(dateKey);
        AtomicInteger counter = fallbackQuota.computeIfAbsent(quotaKey(dateKey, userId), k -> new AtomicInteger(0));
        while (true) {
            int current = counter.get();
            if (current >= dailyLimit) {
                return new QuotaConsumptionResult(false, 0);
            }
            int next = current + 1;
            if (counter.compareAndSet(current, next)) {
                return new QuotaConsumptionResult(true, Math.max(0, dailyLimit - next));
            }
        }
    }

    private void mirrorFallbackQuota(String dateKey, Long userId, int usedCount) {
        resetFallbackIfNewDay(dateKey);
        fallbackQuota.computeIfAbsent(quotaKey(dateKey, userId), k -> new AtomicInteger(0))
                .updateAndGet(current -> Math.max(current, usedCount));
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
