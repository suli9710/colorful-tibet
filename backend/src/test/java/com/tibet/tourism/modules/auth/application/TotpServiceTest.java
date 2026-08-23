package com.tibet.tourism.modules.auth.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.security.CacheKeyHasher;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(OutputCaptureExtension.class)
class TotpServiceTest {

    private static final String RFC_6238_SHA1_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
    private static final String BASE32_ALPHABET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final CacheKeyHasher CACHE_KEY_HASHER = new CacheKeyHasher("test-cache-key-hmac-secret");

    private final TotpService totpService = new TotpService((StringRedisTemplate) null, CACHE_KEY_HASHER);

    @Test
    void generatesRfc6238CompatibleSixDigitCode() {
        String code = totpService.generateCodeForTime(RFC_6238_SHA1_SECRET, Instant.ofEpochSecond(59));

        assertThat(code).isEqualTo("287082");
    }

    @Test
    void validatesBase32SecretStrength() {
        assertThatCode(() -> totpService.validateSecret(RFC_6238_SHA1_SECRET)).doesNotThrowAnyException();
        assertThatCode(() -> totpService.validateSecret("MZXW6YTBOJQXGZJAMZXXE3DEMA"))
                .as("legacy 128-bit secrets remain valid outside strict production policy")
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsInvalidBase32Secret() {
        assertThatThrownBy(() -> totpService.validateSecret("invalid-secret!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("valid Base32");
    }

    @Test
    void rejectsShortSecret() {
        assertThatThrownBy(() -> totpService.validateSecret("JBSWY3DPEHPK3PXP"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("128 bits");

        assertThatThrownBy(() -> totpService.validateSecret(RFC_6238_SHA1_SECRET + "A"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("canonical Base32 encoding");
    }

    @Test
    void rejectsPublishedPlaceholderSecretEvenWhenItLooksLikeBase32() {
        assertThatThrownBy(() -> totpService.validateSecret("replace-with-base32-totp-secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("development placeholder");
    }

    @Test
    void strictValidationRejectsSequentialAndPeriodicSecretsWithoutChangingLegacyPolicy() {
        String sequential = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567";
        String reverseSequential = "765432ZYXWVUTSRQPONMLKJIHGFEDCBA";
        String periodic = "MZXW6YTBOJQXGZJAMZXW6YTBOJQXGZJA";

        assertThatCode(() -> totpService.validateSecret(sequential))
                .as("non-production 128-bit compatibility remains unchanged")
                .doesNotThrowAnyException();
        for (String weakSecret : Set.of(sequential, reverseSequential, periodic)) {
            assertThatThrownBy(() -> totpService.validateStrongSecret(weakSecret))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Production TOTP secret must use high-entropy random material")
                    .hasMessageNotContaining(weakSecret);
        }
    }

    @Test
    void strictValidationAcceptsRuntimeGeneratedCanonicalSecretAndRejectsLowercaseFormatting() {
        String generatedSecret = randomCanonicalBase32Secret();
        String lowercaseSecret = generatedSecret.toLowerCase(Locale.ROOT);

        assertThat(generatedSecret).hasSize(32).matches("[A-Z2-7]{32}");
        assertThatCode(() -> totpService.validateStrongSecret(generatedSecret))
                .doesNotThrowAnyException();
        assertThatCode(() -> totpService.validateSecret(lowercaseSecret))
                .as("legacy non-production parsing remains formatting-compatible")
                .doesNotThrowAnyException();
        assertThatThrownBy(() -> totpService.validateStrongSecret(lowercaseSecret))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("canonical uppercase Base32")
                .hasMessageNotContaining(lowercaseSecret);
    }

    @Test
    void strictValidationRejectsEveryRepositoryKnownPublicOrTestVector() {
        for (String knownVector : Set.of(
                RFC_6238_SHA1_SECRET,
                "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP",
                "MZXW6YTBOJQXGZJAMZXXE3DEMF2GK3LQ",
                "NBSWY3DPEB3W64TMMQXG6ZRAMZXXE3DE",
                "MFRGGZDFMZTWQ2LKMFRGGZDFMZTWQ2LK")) {
            assertThatThrownBy(() -> totpService.validateStrongSecret(knownVector))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Production TOTP secret must use high-entropy random material")
                    .hasMessageNotContaining(knownVector);
        }
    }

    @Test
    void rejectsNonNumericCode() {
        assertThat(totpService.isValidCode(RFC_6238_SHA1_SECRET, "abcdef")).isFalse();
    }

    @Test
    void consumeCodeAcceptsValidCodeOnlyOncePerAccount() {
        String code = totpService.generateCodeForTime(RFC_6238_SHA1_SECRET, Instant.now());

        assertThat(totpService.consumeCode("lzh", RFC_6238_SHA1_SECRET, code)).isTrue();
        assertThat(totpService.consumeCode("lzh", RFC_6238_SHA1_SECRET, code)).isFalse();
    }

    @Test
    void consumeCodeReplayProtectionIsScopedPerAccount() {
        String code = totpService.generateCodeForTime(RFC_6238_SHA1_SECRET, Instant.now());

        assertThat(totpService.consumeCode("account-a", RFC_6238_SHA1_SECRET, code)).isTrue();
        assertThat(totpService.consumeCode("account-b", RFC_6238_SHA1_SECRET, code)).isTrue();
    }

    @Test
    void consumeCodeRejectsAReplayAgainstAnotherInstance() {
        // The in-memory map only covers one JVM, so a captured code could be replayed against a second
        // instance or after a restart. Redis holds the authoritative record of consumed steps.
        Set<String> claimedKeys = new HashSet<>();
        StringRedisTemplate redisTemplate = sharedRedis(claimedKeys);

        TotpService instanceA = new TotpService(redisTemplate, CACHE_KEY_HASHER);
        TotpService instanceB = new TotpService(redisTemplate, CACHE_KEY_HASHER);
        String code = instanceA.generateCodeForTime(RFC_6238_SHA1_SECRET, Instant.now());

        assertThat(instanceA.consumeCode("lzh", RFC_6238_SHA1_SECRET, code)).isTrue();
        assertThat(instanceB.consumeCode("lzh", RFC_6238_SHA1_SECRET, code)).isFalse();
        assertThat(claimedKeys).hasSize(1);
        assertThat(claimedKeys.iterator().next())
                .startsWith("totp-consumed:")
                .doesNotContain("lzh", RFC_6238_SHA1_SECRET);
    }

    @Test
    void replayClaimIsBoundToTheCanonicalCredentialAcrossRotation() {
        String rotatedSecret = "NBSWY3DPEB3W64TMMQXG6ZRAMZXXE3DE";
        Set<String> claimedKeys = new HashSet<>();
        StringRedisTemplate redisTemplate = sharedRedis(claimedKeys);
        TotpService service = new TotpService(redisTemplate, CACHE_KEY_HASHER);
        Instant now = Instant.now();

        String oldCode = service.generateCodeForTime(RFC_6238_SHA1_SECRET, now);
        String rotatedCode = service.generateCodeForTime(rotatedSecret, now);

        assertThat(service.consumeCode("rotating-admin", RFC_6238_SHA1_SECRET, oldCode)).isTrue();
        assertThat(service.consumeCode("rotating-admin", rotatedSecret, rotatedCode)).isTrue();
        assertThat(claimedKeys)
                .hasSize(2)
                .allSatisfy(key -> assertThat(key)
                        .startsWith("totp-consumed:totp-account#")
                        .contains(":totp-credential#")
                        .doesNotContain("rotating-admin", RFC_6238_SHA1_SECRET, rotatedSecret));
    }

    @Test
    void cosmeticSecretFormattingSharesTheSameReplayClaim() {
        Set<String> claimedKeys = new HashSet<>();
        TotpService service = new TotpService(sharedRedis(claimedKeys), CACHE_KEY_HASHER);
        String formattedSecret = "GEZD-GNBV GY3T-QOJQ GEZD-GNBV GY3T-QOJQ";
        String code = service.generateCodeForTime(RFC_6238_SHA1_SECRET, Instant.now());

        assertThat(service.consumeCode("lzh", RFC_6238_SHA1_SECRET, code)).isTrue();
        assertThat(service.consumeCode("lzh", formattedSecret, code)).isFalse();
        assertThat(claimedKeys).hasSize(1);
    }

    @Test
    void consumeCodeStaysUsableWhenRedisIsDown() {
        // Local development can retain the historical single-JVM recovery behavior.
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.opsForValue()).thenThrow(new IllegalStateException("redis down"));

        TotpService service = new TotpService(redisTemplate, CACHE_KEY_HASHER);
        String code = service.generateCodeForTime(RFC_6238_SHA1_SECRET, Instant.now());

        assertThat(service.consumeCode("lzh", RFC_6238_SHA1_SECRET, code)).isTrue();
        assertThat(service.consumeCode("lzh", RFC_6238_SHA1_SECRET, code)).isFalse();
    }

    @Test
    void consumeCodeFailClosedRejectsEveryIndeterminateRedisOutcome(CapturedOutput output) {
        TotpService missingRedis = new TotpService((StringRedisTemplate) null, CACHE_KEY_HASHER, true);
        String missingRedisCode = missingRedis.generateCodeForTime(RFC_6238_SHA1_SECRET, Instant.now());
        assertThat(missingRedis.consumeCode("missing-redis", RFC_6238_SHA1_SECRET, missingRedisCode)).isFalse();

        StringRedisTemplate nullClaimRedis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> nullClaimOperations = mock(ValueOperations.class);
        when(nullClaimRedis.opsForValue()).thenReturn(nullClaimOperations);
        when(nullClaimOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(null);
        TotpService nullClaimService = new TotpService(nullClaimRedis, CACHE_KEY_HASHER, true);
        String nullClaimCode = nullClaimService.generateCodeForTime(RFC_6238_SHA1_SECRET, Instant.now());
        assertThat(nullClaimService.consumeCode("null-claim", RFC_6238_SHA1_SECRET, nullClaimCode)).isFalse();

        StringRedisTemplate failingRedis = mock(StringRedisTemplate.class);
        when(failingRedis.opsForValue()).thenThrow(new IllegalStateException("sensitive redis detail"));
        TotpService failingService = new TotpService(failingRedis, CACHE_KEY_HASHER, true);
        String failingCode = failingService.generateCodeForTime(RFC_6238_SHA1_SECRET, Instant.now());
        assertThat(failingService.consumeCode("failing-redis", RFC_6238_SHA1_SECRET, failingCode)).isFalse();

        assertThat(output.getOut() + output.getErr())
                .contains("redis_not_configured", "redis_no_result", "redis_error")
                .doesNotContain(
                        "missing-redis",
                        "null-claim",
                        "failing-redis",
                        RFC_6238_SHA1_SECRET,
                        missingRedisCode,
                        nullClaimCode,
                        failingCode,
                        "sensitive redis detail");
    }

    @Test
    void consumeCodeFailOpenUsesProcessLocalFallbackForIndeterminateRedisClaim() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any())).thenReturn(null);

        TotpService service = new TotpService(redisTemplate, CACHE_KEY_HASHER, false);
        String code = service.generateCodeForTime(RFC_6238_SHA1_SECRET, Instant.now());

        assertThat(service.consumeCode("local-admin", RFC_6238_SHA1_SECRET, code)).isTrue();
        assertThat(service.consumeCode("local-admin", RFC_6238_SHA1_SECRET, code)).isFalse();
    }

    @SuppressWarnings("unchecked")
    private static StringRedisTemplate sharedRedis(Set<String> claimedKeys) {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any()))
                .thenAnswer(invocation -> claimedKeys.add(invocation.getArgument(0)));
        return redisTemplate;
    }

    private String randomCanonicalBase32Secret() {
        for (int attempt = 0; attempt < 100; attempt++) {
            byte[] bytes = new byte[20];
            SECURE_RANDOM.nextBytes(bytes);
            StringBuilder encoded = new StringBuilder(32);
            int buffer = 0;
            int bitsLeft = 0;
            for (byte rawByte : bytes) {
                buffer = (buffer << 8) | (rawByte & 0xff);
                bitsLeft += 8;
                while (bitsLeft >= 5) {
                    encoded.append(BASE32_ALPHABET.charAt((buffer >> (bitsLeft - 5)) & 31));
                    bitsLeft -= 5;
                    buffer &= (1 << bitsLeft) - 1;
                }
            }
            if (bitsLeft > 0) {
                encoded.append(BASE32_ALPHABET.charAt((buffer << (5 - bitsLeft)) & 31));
            }
            try {
                totpService.validateStrongSecret(encoded.toString());
                return encoded.toString();
            } catch (IllegalArgumentException ignored) {
                // Generate another runtime-only fixture if it happens to match a rejected pattern.
            }
        }
        throw new IllegalStateException("Unable to generate a production-strength TOTP test fixture");
    }

}
