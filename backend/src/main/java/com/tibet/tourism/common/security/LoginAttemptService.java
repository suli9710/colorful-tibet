package com.tibet.tourism.common.security;
import java.util.Arrays;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LoginAttemptService {

    private static final Logger logger = LoggerFactory.getLogger(LoginAttemptService.class);
    private static final String REDIS_PREFIX = "brute-force:";
    private static final String PAIR_SCOPE = "pair:";
    private static final String IP_SCOPE = "ip:";
    private static final String NETWORK_SCOPE = "network:";
    private static final long MAX_LOCK_SECONDS = 7 * 24 * 60 * 60;
    private static final int MAX_INMEMORY_ENTRIES = 10_000;

    private final StringRedisTemplate redisTemplate;
    private final ConcurrentHashMap<String, AttemptRecord> memory = new ConcurrentHashMap<>();

    @Value("${app.security.brute-force.enabled:true}")
    private boolean enabled;

    @Value("${app.security.brute-force.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.security.brute-force.account-step-up-at:${app.security.brute-force.max-attempts:5}}")
    private int accountStepUpAt;

    @Value("${app.security.brute-force.pair-max-attempts:${app.security.brute-force.max-attempts:5}}")
    private int pairMaxAttempts;

    @Value("${app.security.brute-force.ip-max-attempts:30}")
    private int ipMaxAttempts;

    @Value("${app.security.brute-force.network-max-attempts:120}")
    private int networkMaxAttempts;

    @Value("${app.security.brute-force.redis-enabled:true}")
    private boolean redisEnabled;

    private final Set<String> exemptUsernames;

    public LoginAttemptService(
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
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
        return remainingLockSeconds(username) > 0;
    }

    public long remainingLockSeconds(String username) {
        return 0;
    }

    public LoginAttemptDecision evaluate(String username, String clientIp) {
        if (!enabled || username == null || username.isBlank() || isExempt(username)) {
            return LoginAttemptDecision.allowed(false, 0);
        }

        String accountKey = accountKey(username);
        AttemptRecord accountRecord = getRecord(accountKey);
        int accountFailures = accountRecord == null ? 0 : accountRecord.failures;
        boolean stepUpRequired = accountFailures >= safeThreshold(accountStepUpAt);

        LoginAttemptDecision blocked = blockedDecision(clientIp, accountKey);
        if (blocked != null) {
            return blocked.withAccountState(stepUpRequired, accountFailures);
        }

        return LoginAttemptDecision.allowed(stepUpRequired, accountFailures);
    }

    public long remainingLockSeconds(String username, String clientIp) {
        return evaluate(username, clientIp).retryAfterSeconds();
    }

    public boolean accountStepUpRequired(String username) {
        if (!enabled || username == null || username.isBlank() || isExempt(username)) {
            return false;
        }
        AttemptRecord record = getRecord(accountKey(username));
        return record != null && record.failures >= safeThreshold(accountStepUpAt);
    }

    public int failureCount(String username) {
        if (!enabled || username == null || username.isBlank()) {
            return 0;
        }
        AttemptRecord record = getRecord(accountKey(username));
        return record == null ? 0 : record.failures;
    }

    public long recordFailure(String username) {
        LoginAttemptDecision decision = recordFailure(username, "unknown");
        return decision.allowed() ? 0 : decision.retryAfterSeconds();
    }

    public LoginAttemptDecision recordFailure(String username, String clientIp) {
        if (!enabled || username == null || username.isBlank() || isExempt(username)) {
            return LoginAttemptDecision.allowed(false, 0);
        }
        long now = System.currentTimeMillis();

        String accountKey = accountKey(username);
        AttemptRecord accountRecord = incrementRecord(accountKey, now);
        AttemptRecord pairRecord = incrementRecord(pairKey(username, clientIp), now);
        AttemptRecord ipRecord = incrementRecord(ipKey(clientIp), now);
        AttemptRecord networkRecord = incrementRecord(networkKey(clientIp), now);

        evictStaleInMemory(now);

        LoginAttemptDecision blocked = strongestBlockedDecision(
                lockState("pair", pairRecord, safeThreshold(pairMaxAttempts)),
                lockState("ip", ipRecord, safeThreshold(ipMaxAttempts)),
                lockState("network", networkRecord, safeThreshold(networkMaxAttempts)));
        boolean stepUpRequired = accountRecord.failures >= safeThreshold(accountStepUpAt);

        if (blocked != null) {
            logger.warn("Login throttled: username={}, reason={}, accountFailures={}, retryAfterSeconds={}",
                    username, blocked.reason(), accountRecord.failures, blocked.retryAfterSeconds());
            return blocked.withAccountState(stepUpRequired, accountRecord.failures);
        }
        if (stepUpRequired) {
            logger.warn("Login account step-up required: username={}, accountFailures={}",
                    username, accountRecord.failures);
        }
        return LoginAttemptDecision.allowed(stepUpRequired, accountRecord.failures);
    }

    public void reset(String username) {
        if (!enabled || username == null || username.isBlank()) {
            return;
        }
        deleteRecord(accountKey(username));
    }

    public void reset(String username, String clientIp) {
        if (!enabled || username == null || username.isBlank()) {
            return;
        }
        deleteRecord(accountKey(username));
        deleteRecord(pairKey(username, clientIp));
    }

    private LoginAttemptDecision blockedDecision(String clientIp, String accountKey) {
        LoginAttemptDecision blocked = strongestBlockedDecision(
                lockState("ip", getRecord(ipKey(clientIp)), safeThreshold(ipMaxAttempts)),
                lockState("network", getRecord(networkKey(clientIp)), safeThreshold(networkMaxAttempts)));
        if (blocked != null) {
            return blocked;
        }

        String username = accountKey;
        return strongestBlockedDecision(
                lockState("pair", getRecord(pairKeyFromAccountKey(username, clientIp)), safeThreshold(pairMaxAttempts)));
    }

    private LoginAttemptDecision strongestBlockedDecision(LoginAttemptDecision... decisions) {
        LoginAttemptDecision strongest = null;
        for (LoginAttemptDecision decision : decisions) {
            if (decision == null || decision.allowed()) {
                continue;
            }
            if (strongest == null || decision.retryAfterSeconds() > strongest.retryAfterSeconds()) {
                strongest = decision;
            }
        }
        return strongest;
    }

    private LoginAttemptDecision lockState(String reason, AttemptRecord record, int threshold) {
        if (record == null || record.failures < threshold) {
            return null;
        }
        long retryAfter = remainingSeconds(record, threshold);
        if (retryAfter <= 0) {
            return null;
        }
        return LoginAttemptDecision.blocked(reason, retryAfter, 0, false);
    }

    private AttemptRecord incrementRecord(String key, long now) {
        AttemptRecord redisRecord = readFromRedis(key);
        AttemptRecord newRecord = memory.compute(key, (k, existing) -> {
            AttemptRecord base = mergeMaxFailures(redisRecord, existing);
            int newFailures = (base == null) ? 1 : base.failures + 1;
            return new AttemptRecord(newFailures, now);
        });
        syncToRedis(key, newRecord);
        return newRecord;
    }

    private AttemptRecord getRecord(String key) {
        if (redisEnabled && redisTemplate != null) {
            try {
                String value = redisTemplate.opsForValue().get(REDIS_PREFIX + key);
                if (value == null) {
                    memory.remove(key);
                    return null;
                }

                AttemptRecord redisRecord = fromRedisValue(value);
                if (redisRecord != null) {
                    return mergeMaxFailures(redisRecord, memory.get(key));
                }
                logger.debug("Ignoring unreadable brute-force Redis payload for key={}", key);
            } catch (Exception e) {
                logger.debug("Redis unavailable for brute-force check; falling back to in-memory: {}", e.getMessage());
            }
        }
        return memory.get(key);
    }

    private AttemptRecord readFromRedis(String key) {
        if (redisEnabled && redisTemplate != null) {
            try {
                String value = redisTemplate.opsForValue().get(REDIS_PREFIX + key);
                if (value != null) {
                    return fromRedisValue(value);
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

    private String accountKey(String username) {
        return username.trim().toLowerCase();
    }

    private String pairKey(String username, String clientIp) {
        return pairKeyFromAccountKey(accountKey(username), clientIp);
    }

    private String pairKeyFromAccountKey(String accountKey, String clientIp) {
        return PAIR_SCOPE + accountKey + ":" + shortHash(normalizeClientIp(clientIp));
    }

    private String ipKey(String clientIp) {
        return IP_SCOPE + shortHash(normalizeClientIp(clientIp));
    }

    private String networkKey(String clientIp) {
        return NETWORK_SCOPE + shortHash(networkPrefix(normalizeClientIp(clientIp)));
    }

    private String normalizeClientIp(String clientIp) {
        if (!StringUtils.hasText(clientIp)) {
            return "unknown";
        }
        String normalized = clientIp.trim();
        if (normalized.contains(",")) {
            normalized = normalized.split(",")[0].trim();
        }
        if ("0:0:0:0:0:0:0:1".equals(normalized)) {
            return "127.0.0.1";
        }
        return normalized.length() > 128 ? shortHash(normalized) : normalized;
    }

    private String networkPrefix(String clientIp) {
        String[] parts = clientIp.split("\\.");
        if (parts.length == 4 && isIpv4Part(parts[0]) && isIpv4Part(parts[1])
                && isIpv4Part(parts[2]) && isIpv4Part(parts[3])) {
            return parts[0] + "." + parts[1] + "." + parts[2] + ".0/24";
        }
        return clientIp;
    }

    private boolean isIpv4Part(String value) {
        try {
            int parsed = Integer.parseInt(value);
            return parsed >= 0 && parsed <= 255;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private String shortHash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest((value == null ? "" : value).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed, 0, 8);
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static final long[] LOCK_PROGRESSION = {
        30,          // excess 0: 30秒
        5 * 60,      // excess 1: 5分钟
        30 * 60,     // excess 2: 30分钟
        12 * 3600,   // excess 3: 12小时
        7 * 86400    // excess 4: 7天
    };

    private int safeThreshold(int threshold) {
        return Math.max(1, threshold);
    }

    private long remainingSeconds(AttemptRecord record, int threshold) {
        long lockSeconds = lockSecondsFor(record.failures, threshold);
        long elapsed = (System.currentTimeMillis() - record.lastFailureAt) / 1000;
        return Math.max(0, lockSeconds - elapsed);
    }

    private long lockSecondsFor(int failures, int threshold) {
        int excess = failures - threshold;
        if (excess < 0) {
            return 0;
        }
        if (excess < LOCK_PROGRESSION.length) {
            return LOCK_PROGRESSION[excess];
        }
        return LOCK_PROGRESSION[LOCK_PROGRESSION.length - 1];
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

    public record LoginAttemptDecision(
            boolean allowed,
            boolean stepUpRequired,
            String reason,
            long retryAfterSeconds,
            int accountFailures) {
        static LoginAttemptDecision allowed(boolean stepUpRequired, int accountFailures) {
            return new LoginAttemptDecision(true, stepUpRequired, "", 0, accountFailures);
        }

        static LoginAttemptDecision blocked(String reason, long retryAfterSeconds,
                                            int accountFailures, boolean stepUpRequired) {
            return new LoginAttemptDecision(false, stepUpRequired, reason,
                    Math.max(1, retryAfterSeconds), accountFailures);
        }

        LoginAttemptDecision withAccountState(boolean stepUpRequired, int accountFailures) {
            return new LoginAttemptDecision(
                    allowed,
                    stepUpRequired,
                    reason,
                    retryAfterSeconds,
                    accountFailures);
        }
    }
}
