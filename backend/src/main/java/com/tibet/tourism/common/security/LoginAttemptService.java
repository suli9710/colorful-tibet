package com.tibet.tourism.common.security;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LoginAttemptService {

    private static final Logger logger = LoggerFactory.getLogger(LoginAttemptService.class);
    private static final String REDIS_PREFIX = "brute-force:";
    private static final long MAX_LOCK_SECONDS = 7 * 24 * 60 * 60;
    private static final int MAX_INMEMORY_ENTRIES = 10_000;

    private final RedisTemplate<String, Object> redisTemplate;
    private final ConcurrentHashMap<String, AttemptRecord> memory = new ConcurrentHashMap<>();

    @Value("${app.security.brute-force.enabled:true}")
    private boolean enabled;

    @Value("${app.security.brute-force.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.security.brute-force.redis-enabled:true}")
    private boolean redisEnabled;

    private final Set<String> exemptUsernames;

    public LoginAttemptService(
            ObjectProvider<RedisTemplate<String, Object>> redisTemplateProvider,
            @Value("${app.security.brute-force.exempt-usernames:}") String exemptUsernamesConfig) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
        Set<String> exempt = new java.util.HashSet<>();
        if (StringUtils.hasText(exemptUsernamesConfig)) {
            Arrays.stream(exemptUsernamesConfig.split(","))
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .map(String::toLowerCase)
                    .forEach(exempt::add);
        }
        this.exemptUsernames = Set.copyOf(exempt);
    }

    private boolean isExempt(String username) {
        return exemptUsernames.contains(username.trim().toLowerCase());
    }

    public boolean isLocked(String username) {
        if (!enabled || username == null || username.isBlank() || isExempt(username)) {
            return false;
        }
        String key = normalizeKey(username);

        AttemptRecord record = getRecord(key);
        if (record == null || record.failures < maxAttempts) {
            return false;
        }

        long lockSeconds = lockSecondsFor(record.failures);
        long elapsed = (System.currentTimeMillis() - record.lastFailureAt) / 1000;
        return elapsed < lockSeconds;
    }

    public long remainingLockSeconds(String username) {
        if (!enabled || username == null || username.isBlank() || isExempt(username)) {
            return 0;
        }
        String key = normalizeKey(username);

        AttemptRecord record = getRecord(key);
        if (record == null || record.failures < maxAttempts) {
            return 0;
        }

        long lockSeconds = lockSecondsFor(record.failures);
        long elapsed = (System.currentTimeMillis() - record.lastFailureAt) / 1000;
        return Math.max(0, lockSeconds - elapsed);
    }

    public int failureCount(String username) {
        if (!enabled || username == null || username.isBlank()) {
            return 0;
        }
        AttemptRecord record = getRecord(normalizeKey(username));
        return record == null ? 0 : record.failures;
    }

    public long recordFailure(String username) {
        if (!enabled || username == null || username.isBlank() || isExempt(username)) {
            return 0;
        }
        String key = normalizeKey(username);
        long now = System.currentTimeMillis();

        AttemptRecord redisRecord = readFromRedis(key);

        AttemptRecord newRecord = memory.compute(key, (k, existing) -> {
            AttemptRecord base = mergeMaxFailures(redisRecord, existing);
            int newFailures = (base == null) ? 1 : base.failures + 1;
            return new AttemptRecord(newFailures, now);
        });

        syncToRedis(key, newRecord);
        evictStaleInMemory(now);

        if (newRecord.failures >= maxAttempts) {
            long lockSeconds = lockSecondsFor(newRecord.failures);
            logger.warn("Account locked: username={}, failures={}, lockSeconds={}", username, newRecord.failures, lockSeconds);
            return lockSeconds;
        }
        return 0;
    }

    public void reset(String username) {
        if (!enabled || username == null || username.isBlank()) {
            return;
        }
        String key = normalizeKey(username);
        deleteRecord(key);
    }

    private AttemptRecord getRecord(String key) {
        if (redisEnabled && redisTemplate != null) {
            try {
                Object value = redisTemplate.opsForValue().get(REDIS_PREFIX + key);
                if (value == null) {
                    memory.remove(key);
                    return null;
                }

                AttemptRecord redisRecord = toAttemptRecord(value);
                if (redisRecord != null) {
                    return mergeMaxFailures(redisRecord, memory.get(key));
                }
                logger.debug("Ignoring unreadable brute-force Redis payload for key={}, type={}",
                        key, value.getClass().getName());
            } catch (Exception e) {
                logger.debug("Redis unavailable for brute-force check; falling back to in-memory: {}", e.getMessage());
            }
        }
        return memory.get(key);
    }

    private AttemptRecord readFromRedis(String key) {
        if (redisEnabled && redisTemplate != null) {
            try {
                Object value = redisTemplate.opsForValue().get(REDIS_PREFIX + key);
                if (value != null) {
                    return toAttemptRecord(value);
                }
            } catch (Exception e) {
                logger.debug("Redis read failed for brute-force key={}: {}", key, e.getMessage());
            }
        }
        return null;
    }

    private void syncToRedis(String key, AttemptRecord record) {
        if (redisEnabled && redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(REDIS_PREFIX + key, record.toRedisValue(), MAX_LOCK_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.debug("Redis unavailable for brute-force save: {}", e.getMessage());
            }
        }
    }

    private void evictStaleInMemory(long now) {
        if (memory.size() >= MAX_INMEMORY_ENTRIES) {
            long cutoff = now - TimeUnit.HOURS.toMillis(1);
            memory.entrySet().removeIf(entry -> entry.getValue().lastFailureAt < cutoff);
        }
    }

    private void deleteRecord(String key) {
        if (redisEnabled && redisTemplate != null) {
            try {
                redisTemplate.delete(REDIS_PREFIX + key);
            } catch (Exception e) {
                logger.debug("Redis unavailable for brute-force delete: {}", e.getMessage());
            }
        }
        memory.remove(key);
    }

    private AttemptRecord mergeMaxFailures(AttemptRecord a, AttemptRecord b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.failures >= b.failures ? a : b;
    }

    private String normalizeKey(String username) {
        return username.trim().toLowerCase();
    }

    private static final long[] LOCK_PROGRESSION = {
        30,          // excess 0: 30秒
        5 * 60,      // excess 1: 5分钟
        30 * 60,     // excess 2: 30分钟
        12 * 3600,   // excess 3: 12小时
        7 * 86400    // excess 4: 7天
    };

    private long lockSecondsFor(int failures) {
        int excess = failures - maxAttempts;
        if (excess < 0) {
            return 0;
        }
        if (excess < LOCK_PROGRESSION.length) {
            return LOCK_PROGRESSION[excess];
        }
        return LOCK_PROGRESSION[LOCK_PROGRESSION.length - 1];
    }

    private AttemptRecord toAttemptRecord(Object value) {
        if (value instanceof AttemptRecord record) {
            return record;
        }
        if (value instanceof String text) {
            return fromRedisValue(text);
        }
        if (value instanceof Map<?, ?> source) {
            Integer failures = toInteger(source.get("failures"));
            Long lastFailureAt = toLong(source.get("lastFailureAt"));
            return newAttemptRecord(failures, lastFailureAt);
        }
        return null;
    }

    private AttemptRecord fromRedisValue(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        int separator = value.indexOf(':');
        if (separator <= 0 || separator == value.length() - 1) {
            return null;
        }
        Integer failures = toInteger(value.substring(0, separator));
        Long lastFailureAt = toLong(value.substring(separator + 1));
        return newAttemptRecord(failures, lastFailureAt);
    }

    private AttemptRecord newAttemptRecord(Integer failures, Long lastFailureAt) {
        if (failures == null || failures <= 0 || lastFailureAt == null || lastFailureAt <= 0) {
            return null;
        }
        return new AttemptRecord(failures, lastFailureAt);
    }

    private Integer toInteger(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text) {
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private record AttemptRecord(int failures, long lastFailureAt) {
        String toRedisValue() {
            return failures + ":" + lastFailureAt;
        }
    }
}
