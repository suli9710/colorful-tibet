package com.tibet.tourism.common.security;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.concurrent.TimeUnit;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class LoginAttemptService {

    private static final Logger logger = LoggerFactory.getLogger(LoginAttemptService.class);
    private static final String REDIS_PREFIX = "brute-force:";
    private static final String ACCOUNT_SCOPE = "acct:";
    private static final String PAIR_SCOPE = "pair:";
    private static final String IP_SCOPE = "ip:";
    private static final String NETWORK_SCOPE = "network:";
    private static final long MAX_LOCK_SECONDS = 7 * 24 * 60 * 60;
    private static final long BACKEND_UNAVAILABLE_RETRY_SECONDS = 60;
    private static final int MAX_INMEMORY_ENTRIES = 10_000;
    static final String INCREMENT_ATTEMPT_LUA =
            """
            local current = redis.call('GET', KEYS[1])
            local failures = 0
            if current then
              local sep = string.find(current, ':', 1, true)
              if sep then
                failures = tonumber(string.sub(current, 1, sep - 1)) or 0
              end
            end
            failures = failures + 1
            local newValue = failures .. ':' .. ARGV[1]
            redis.call('SET', KEYS[1], newValue, 'EX', tonumber(ARGV[2]))
            return newValue
            """;
    private static final RedisScript<String> INCREMENT_ATTEMPT_SCRIPT = RedisScript.of(
            INCREMENT_ATTEMPT_LUA,
            String.class);

    private final StringRedisTemplate redisTemplate;
    private final Cache<String, AttemptRecord> memory = Caffeine.newBuilder()
            .maximumSize(MAX_INMEMORY_ENTRIES)
            .expireAfterWrite(Duration.ofDays(7))
            .build();

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

    @Value("${app.security.brute-force.redis-fail-closed:false}")
    private boolean redisFailClosed;

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
        if (!enabled || username == null || username.isBlank() || isExempt(username)) {
            return 0;
        }
        try {
            AttemptRecord record = getRecord(accountKey(username));
            return record == null ? 0 : remainingSeconds(record, safeThreshold(maxAttempts));
        } catch (ProtectionBackendUnavailableException e) {
            return BACKEND_UNAVAILABLE_RETRY_SECONDS;
        }
    }

    public LoginAttemptDecision evaluate(String username, String clientIp) {
        if (!enabled || username == null || username.isBlank() || isExempt(username)) {
            return LoginAttemptDecision.allowed(false, 0);
        }

        try {
            String accountKey = accountKey(username);
            AttemptRecord accountRecord = getRecord(accountKey);
            int accountFailures = accountRecord == null ? 0 : accountRecord.failures;
            boolean stepUpRequired = accountFailures >= safeThreshold(accountStepUpAt);

            LoginAttemptDecision blocked = strongestBlockedDecision(
                    lockState("account", accountRecord, safeThreshold(maxAttempts)),
                    blockedDecision(clientIp, accountKey));
            if (blocked != null) {
                return blocked.withAccountState(stepUpRequired, accountFailures);
            }

            return LoginAttemptDecision.allowed(stepUpRequired, accountFailures);
        } catch (ProtectionBackendUnavailableException e) {
            logger.warn("Login throttle backend unavailable during evaluation: user={}", userLabel(username));
            return backendUnavailableDecision();
        }
    }

    public long remainingLockSeconds(String username, String clientIp) {
        return evaluate(username, clientIp).retryAfterSeconds();
    }

    public boolean accountStepUpRequired(String username) {
        if (!enabled || username == null || username.isBlank() || isExempt(username)) {
            return false;
        }
        try {
            AttemptRecord record = getRecord(accountKey(username));
            return record != null && record.failures >= safeThreshold(accountStepUpAt);
        } catch (ProtectionBackendUnavailableException e) {
            return true;
        }
    }

    public int failureCount(String username) {
        if (!enabled || username == null || username.isBlank()) {
            return 0;
        }
        try {
            AttemptRecord record = getRecord(accountKey(username));
            return record == null ? 0 : record.failures;
        } catch (ProtectionBackendUnavailableException e) {
            return safeThreshold(maxAttempts);
        }
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
        AttemptRecord accountRecord;
        AttemptRecord pairRecord;
        AttemptRecord ipRecord;
        AttemptRecord networkRecord;
        try {
            accountRecord = incrementRecord(accountKey, now);
            pairRecord = incrementRecord(pairKey(username, clientIp), now);
            ipRecord = incrementRecord(ipKey(clientIp), now);
            networkRecord = incrementRecord(networkKey(clientIp), now);
        } catch (ProtectionBackendUnavailableException e) {
            logger.warn("Login throttle backend unavailable while recording failure: user={}", userLabel(username));
            return backendUnavailableDecision();
        }

        LoginAttemptDecision blocked = strongestBlockedDecision(
                lockState("account", accountRecord, safeThreshold(maxAttempts)),
                lockState("pair", pairRecord, safeThreshold(pairMaxAttempts)),
                lockState("ip", ipRecord, safeThreshold(ipMaxAttempts)),
                lockState("network", networkRecord, safeThreshold(networkMaxAttempts)));
        boolean stepUpRequired = accountRecord.failures >= safeThreshold(accountStepUpAt);

        if (blocked != null) {
            logger.warn("Login throttled: user={}, reason={}, accountFailures={}, retryAfterSeconds={}",
                    userLabel(username), blocked.reason(), accountRecord.failures, blocked.retryAfterSeconds());
            return blocked.withAccountState(stepUpRequired, accountRecord.failures);
        }
        if (stepUpRequired) {
            logger.warn("Login account step-up required: user={}, accountFailures={}",
                    userLabel(username), accountRecord.failures);
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
        AttemptRecord redisRecord = incrementRecordInRedis(key, now);
        if (redisRecord != null) {
            memory.put(key, redisRecord);
            return redisRecord;
        }

        AttemptRecord newRecord = memory.asMap().compute(key, (k, existing) -> {
            int newFailures = (existing == null) ? 1 : existing.failures + 1;
            return new AttemptRecord(newFailures, now);
        });
        syncToRedis(key, newRecord);
        return newRecord;
    }

    private AttemptRecord incrementRecordInRedis(String key, long now) {
        if (!redisEnabled) {
            return null;
        }
        if (redisTemplate == null) {
            failClosedIfRedisUnavailable(null);
            return null;
        }
        try {
            String result = redisTemplate.execute(
                    INCREMENT_ATTEMPT_SCRIPT,
                    List.of(REDIS_PREFIX + key),
                    String.valueOf(now),
                    String.valueOf(MAX_LOCK_SECONDS));
            return fromRedisValue(result);
        } catch (Exception e) {
            logger.debug("Redis unavailable for brute-force increment; falling back to in-memory: {}",
                    SensitiveLogSanitizer.exceptionSummary(e));
            if (redisFailClosed) {
                throw new ProtectionBackendUnavailableException(e);
            }
            return null;
        }
    }

    private AttemptRecord getRecord(String key) {
        if (redisEnabled) {
            if (redisTemplate == null) {
                failClosedIfRedisUnavailable(null);
                return memory.getIfPresent(key);
            }
            try {
                String value = redisTemplate.opsForValue().get(REDIS_PREFIX + key);
                if (value == null) {
                    memory.invalidate(key);
                    return null;
                }

                AttemptRecord redisRecord = fromRedisValue(value);
                if (redisRecord != null) {
                    return mergeMaxFailures(redisRecord, memory.getIfPresent(key));
                }
                logger.debug("Ignoring unreadable brute-force Redis payload for keyHash={}", shortHash(key));
            } catch (Exception e) {
                logger.debug("Redis unavailable for brute-force check; falling back to in-memory: {}",
                        SensitiveLogSanitizer.exceptionSummary(e));
                if (redisFailClosed) {
                    throw new ProtectionBackendUnavailableException(e);
                }
            }
        }
        return memory.getIfPresent(key);
    }

    private void syncToRedis(String key, AttemptRecord record) {
        if (redisEnabled) {
            if (redisTemplate == null) {
                failClosedIfRedisUnavailable(null);
                return;
            }
            try {
                redisTemplate.opsForValue().set(REDIS_PREFIX + key, record.toRedisValue(), MAX_LOCK_SECONDS, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.debug("Redis unavailable for brute-force save: {}", SensitiveLogSanitizer.exceptionSummary(e));
                if (redisFailClosed) {
                    throw new ProtectionBackendUnavailableException(e);
                }
            }
        }
    }

    private void failClosedIfRedisUnavailable(Throwable cause) {
        if (redisFailClosed) {
            throw new ProtectionBackendUnavailableException(cause);
        }
    }

    private LoginAttemptDecision backendUnavailableDecision() {
        return LoginAttemptDecision.blocked("backend", BACKEND_UNAVAILABLE_RETRY_SECONDS, 0, false);
    }

    private void deleteRecord(String key) {
        if (redisEnabled && redisTemplate != null) {
            try {
                redisTemplate.delete(REDIS_PREFIX + key);
            } catch (Exception e) {
                logger.debug("Redis unavailable for brute-force delete: {}", SensitiveLogSanitizer.exceptionSummary(e));
            }
        }
        memory.invalidate(key);
    }

    long inMemoryMaximumSize() {
        return memory.policy().eviction().orElseThrow().getMaximum();
    }

    private AttemptRecord mergeMaxFailures(AttemptRecord a, AttemptRecord b) {
        if (a == null) return b;
        if (b == null) return a;
        return a.failures >= b.failures ? a : b;
    }

    private String accountKey(String username) {
        return ACCOUNT_SCOPE + shortHash(normalizeUsername(username));
    }

    private String pairKey(String username, String clientIp) {
        return pairKeyFromAccountKey(accountKey(username), clientIp);
    }

    private String pairKeyFromAccountKey(String accountKey, String clientIp) {
        return PAIR_SCOPE + accountKey + ":" + shortHash(normalizeClientIp(clientIp));
    }

    private String userLabel(String username) {
        return "user#" + shortHash(normalizeUsername(username));
    }

    private String normalizeUsername(String username) {
        return username == null ? "unknown" : username.trim().toLowerCase();
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

    private static final class ProtectionBackendUnavailableException extends RuntimeException {
        ProtectionBackendUnavailableException(Throwable cause) {
            super(cause);
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
