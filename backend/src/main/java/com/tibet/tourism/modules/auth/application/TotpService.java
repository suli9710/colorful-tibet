package com.tibet.tourism.modules.auth.application;

import com.tibet.tourism.common.security.CacheKeyHasher;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TotpService {

    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final int PERIOD_SECONDS = 30;
    private static final int DIGITS = 6;
    private static final int ACCEPTED_WINDOW_STEPS = 1;
    private static final int MIN_SECRET_BYTES = 16;
    private static final int MIN_STRONG_SECRET_BYTES = 20;
    private static final int OTP_MODULO = 1_000_000;
    private static final int MIN_STRONG_SECRET_SYMBOLS = 8;
    private static final Set<String> KNOWN_PUBLIC_OR_TEST_SECRETS = Set.of(
            "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ",
            "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP",
            "MZXW6YTBOJQXGZJAMZXXE3DEMF2GK3LQ",
            "NBSWY3DPEB3W64TMMQXG6ZRAMZXXE3DE",
            "MFRGGZDFMZTWQ2LKMFRGGZDFMZTWQ2LK");
    private static final String CONSUMED_STEP_KEY_PREFIX = "totp-consumed:";
    // Comfortably outlives the accepted window (current step plus one either side).
    private static final Duration CONSUMED_STEP_TTL = Duration.ofSeconds(PERIOD_SECONDS * (2L * ACCEPTED_WINDOW_STEPS + 2));

    private static final Logger logger = LoggerFactory.getLogger(TotpService.class);

    // Same-process fast path. On its own this only protects a single JVM: a captured code could still
    // be replayed against another instance or after a restart, so Redis below is the authoritative
    // record whenever it is available.
    private final Map<String, Long> lastConsumedStep = new ConcurrentHashMap<>();

    private final StringRedisTemplate redisTemplate;
    private final CacheKeyHasher cacheKeyHasher;
    private final boolean replayFailClosed;

    @Autowired
    public TotpService(
            ObjectProvider<StringRedisTemplate> redisTemplateProvider,
            CacheKeyHasher cacheKeyHasher,
            @Value("${app.security.totp-replay-fail-closed:false}") boolean replayFailClosed) {
        this(redisTemplateProvider.getIfAvailable(), cacheKeyHasher, replayFailClosed);
    }

    /**
     * Direct constructor for tests and local callers that already resolved the Redis template.
     * Keeps the historical process-local fallback; production wiring uses the configured policy.
     */
    public TotpService(StringRedisTemplate redisTemplate, CacheKeyHasher cacheKeyHasher) {
        this(redisTemplate, cacheKeyHasher, false);
    }

    /** Direct constructor for callers that need to exercise an explicit replay-backend policy. */
    public TotpService(
            StringRedisTemplate redisTemplate,
            CacheKeyHasher cacheKeyHasher,
            boolean replayFailClosed) {
        this.redisTemplate = redisTemplate;
        this.cacheKeyHasher = cacheKeyHasher;
        this.replayFailClosed = replayFailClosed;
    }

    public void validateSecret(String base32Secret) {
        rejectPlaceholderSecret(base32Secret);
        byte[] decoded = decodeBase32(base32Secret);
        if (decoded.length < MIN_SECRET_BYTES) {
            throw new IllegalArgumentException("TOTP secret must contain at least 128 bits of entropy");
        }
    }

    /** Applies production-strength checks in addition to Base32 shape and minimum decoded length. */
    public void validateStrongSecret(String base32Secret) {
        validateSecret(base32Secret);
        String canonical = canonicalSecret(base32Secret);
        if (!base32Secret.equals(canonical)) {
            throw new IllegalArgumentException(
                    "Production TOTP secret must use canonical uppercase Base32 without separators or padding");
        }
        if (canonical.length() < 32
                || canonical.length() % 8 != 0
                || decodeBase32(base32Secret).length < MIN_STRONG_SECRET_BYTES) {
            throw new IllegalArgumentException(
                    "Production TOTP secret must contain at least 160 bits in complete Base32 blocks");
        }
        long distinctSymbols = canonical.chars().distinct().count();
        if (distinctSymbols < MIN_STRONG_SECRET_SYMBOLS
                || KNOWN_PUBLIC_OR_TEST_SECRETS.contains(canonical)
                || isSequentialBase32Pattern(canonical)
                || isPeriodicPattern(canonical)) {
            throw new IllegalArgumentException(
                    "Production TOTP secret must use high-entropy random material");
        }
    }

    public boolean isValidCode(String base32Secret, String providedCode) {
        return matchStep(base32Secret, providedCode).isPresent();
    }

    /**
     * Validates a code and, on success, atomically marks its time step as consumed for the given
     * account so the same (or an older) step cannot be reused. Returns false when the code is invalid
     * or has already been consumed (replay).
     *
     * <p>When Redis is reachable the consumed step is recorded there, so the guarantee holds across
     * application restarts and across every instance behind the load balancer. Production runs
     * fail closed when Redis cannot give an authoritative claim. Local development may explicitly
     * retain process-local fallback behavior.
     */
    public boolean consumeCode(String accountKey, String base32Secret, String providedCode) {
        OptionalLong matched = matchStep(base32Secret, providedCode);
        if (matched.isEmpty()) {
            return false;
        }
        long matchedStep = matched.getAsLong();
        String replayScope = replayScope(accountKey, base32Secret);
        boolean[] accepted = {false};
        lastConsumedStep.compute(replayScope, (key, previous) -> {
            if (previous != null && previous >= matchedStep) {
                return previous;
            }
            accepted[0] = true;
            return matchedStep;
        });
        if (!accepted[0]) {
            return false;
        }
        return claimStepAcrossInstances(replayScope, matchedStep);
    }

    private boolean claimStepAcrossInstances(String replayScope, long matchedStep) {
        if (redisTemplate == null) {
            logger.warn("TOTP replay protection backend unavailable: redis_not_configured");
            return !replayFailClosed;
        }
        String key = CONSUMED_STEP_KEY_PREFIX + replayScope + ":" + matchedStep;
        try {
            Boolean claimed = redisTemplate.opsForValue().setIfAbsent(key, "1", CONSUMED_STEP_TTL);
            if (Boolean.FALSE.equals(claimed)) {
                return false;
            }
            if (claimed == null) {
                logger.warn("TOTP replay protection backend unavailable: redis_no_result");
                return !replayFailClosed;
            }
            return true;
        } catch (RuntimeException e) {
            logger.warn("TOTP replay protection backend unavailable: redis_error");
            return !replayFailClosed;
        }
    }

    /**
     * Separates replay claims by both account and the currently configured credential. This keeps
     * a consumed step from the old credential from blocking the first code after an atomic secret
     * rotation, while canonicalization makes cosmetic Base32 formatting changes retain the same
     * replay boundary. Only keyed, irreversible labels are retained locally or sent to Redis.
     */
    private String replayScope(String accountKey, String base32Secret) {
        return cacheKeyHasher.cacheKey("totp-account", accountKey)
                + ":"
                + cacheKeyHasher.cacheKey("totp-credential", canonicalSecret(base32Secret));
    }

    private OptionalLong matchStep(String base32Secret, String providedCode) {
        if (!StringUtils.hasText(providedCode) || !providedCode.matches("\\d{" + DIGITS + "}")) {
            return OptionalLong.empty();
        }

        byte[] secret = decodeBase32(base32Secret);
        long currentStep = Instant.now().getEpochSecond() / PERIOD_SECONDS;
        for (int offset = -ACCEPTED_WINDOW_STEPS; offset <= ACCEPTED_WINDOW_STEPS; offset++) {
            long step = currentStep + offset;
            String expected = generateCode(secret, step);
            if (MessageDigest.isEqual(
                    expected.getBytes(StandardCharsets.UTF_8),
                    providedCode.getBytes(StandardCharsets.UTF_8))) {
                return OptionalLong.of(step);
            }
        }
        return OptionalLong.empty();
    }

    String generateCodeForTime(String base32Secret, Instant instant) {
        return generateCode(decodeBase32(base32Secret), instant.getEpochSecond() / PERIOD_SECONDS);
    }

    private void rejectPlaceholderSecret(String base32Secret) {
        if (!StringUtils.hasText(base32Secret)) {
            throw new IllegalArgumentException("TOTP secret must be configured");
        }

        String normalized = base32Secret.trim().toLowerCase(Locale.ROOT);
        if (normalized.contains("replace-with")
                || normalized.contains("placeholder")
                || normalized.contains("changeme")
                || normalized.contains("change-me")) {
            throw new IllegalArgumentException("TOTP secret must not use a development placeholder");
        }
    }

    private String generateCode(byte[] secret, long timeStep) {
        byte[] counter = new byte[8];
        long value = timeStep;
        for (int i = counter.length - 1; i >= 0; i--) {
            counter[i] = (byte) (value & 0xff);
            value >>= 8;
        }

        byte[] hash = hmacSha1(secret, counter);
        int offset = hash[hash.length - 1] & 0x0f;
        int binary = ((hash[offset] & 0x7f) << 24)
                | ((hash[offset + 1] & 0xff) << 16)
                | ((hash[offset + 2] & 0xff) << 8)
                | (hash[offset + 3] & 0xff);
        int otp = binary % OTP_MODULO;
        return String.format(Locale.ROOT, "%0" + DIGITS + "d", otp);
    }

    private byte[] hmacSha1(byte[] secret, byte[] message) {
        try {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret, "HmacSHA1"));
            return mac.doFinal(message);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("Unable to calculate TOTP code", exception);
        }
    }

    private byte[] decodeBase32(String base32Secret) {
        if (!StringUtils.hasText(base32Secret)) {
            throw new IllegalArgumentException("TOTP secret must be configured");
        }

        String normalized = base32Secret.replace(" ", "")
                .replace("-", "")
                .replace("=", "")
                .toUpperCase(Locale.ROOT);
        int buffer = 0;
        int bitsLeft = 0;
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        for (int i = 0; i < normalized.length(); i++) {
            char ch = normalized.charAt(i);
            int value = BASE32_ALPHABET.indexOf(ch);
            if (value < 0) {
                throw new IllegalArgumentException("TOTP secret must be valid Base32");
            }

            buffer = (buffer << 5) | value;
            bitsLeft += 5;
            if (bitsLeft >= 8) {
                output.write((buffer >> (bitsLeft - 8)) & 0xff);
                bitsLeft -= 8;
                buffer &= (1 << bitsLeft) - 1;
            }
        }

        int blockRemainder = normalized.length() % 8;
        if ((blockRemainder != 0
                        && blockRemainder != 2
                        && blockRemainder != 4
                        && blockRemainder != 5
                        && blockRemainder != 7)
                || (bitsLeft > 0 && buffer != 0)) {
            throw new IllegalArgumentException("TOTP secret must use a valid canonical Base32 encoding");
        }

        return output.toByteArray();
    }

    private String canonicalSecret(String base32Secret) {
        return base32Secret.replace(" ", "")
                .replace("-", "")
                .replace("=", "")
                .toUpperCase(Locale.ROOT);
    }

    private boolean isSequentialBase32Pattern(String canonical) {
        boolean ascending = true;
        boolean descending = true;
        for (int i = 1; i < canonical.length() && (ascending || descending); i++) {
            int previous = BASE32_ALPHABET.indexOf(canonical.charAt(i - 1));
            int current = BASE32_ALPHABET.indexOf(canonical.charAt(i));
            ascending &= current == (previous + 1) % BASE32_ALPHABET.length();
            descending &= current == (previous - 1 + BASE32_ALPHABET.length())
                    % BASE32_ALPHABET.length();
        }
        return ascending || descending;
    }

    private boolean isPeriodicPattern(String canonical) {
        for (int period = 1; period <= canonical.length() / 2; period++) {
            if (canonical.length() % period != 0) {
                continue;
            }
            boolean repeated = true;
            for (int i = period; i < canonical.length(); i++) {
                if (canonical.charAt(i) != canonical.charAt(i % period)) {
                    repeated = false;
                    break;
                }
            }
            if (repeated) {
                return true;
            }
        }
        return false;
    }
}
