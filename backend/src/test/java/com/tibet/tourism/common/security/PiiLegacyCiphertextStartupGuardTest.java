package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class PiiLegacyCiphertextStartupGuardTest {

    @AfterEach
    void resetKeys() {
        PiiCryptoConverter.configure("", "", "");
        PiiCryptoConverter.configureMetrics(null);
    }

    @Test
    void failsStartupWhenLegacyRowsExistWithoutLegacyKey() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq("enc:v1:%")))
                .thenReturn(0L, 2L);

        PiiLegacyCiphertextStartupGuard guard = new PiiLegacyCiphertextStartupGuard(jdbcTemplate, true);

        assertThatThrownBy(() -> guard.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PII_ENCRYPTION_KEY is required")
                .hasMessageContaining("hotel_bookings.phone=2");
    }

    @Test
    void rejectsPlaceholderLegacyKeyAtStartupConfiguration() {
        assertThatThrownBy(() -> PiiCryptoConverter.configure("", "", "replace-with-at-least-64-random-characters"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Legacy PII encryption key")
                .hasMessageContaining("placeholder");
    }

    @Test
    void failsStartupWhenLegacyRowsCannotBeDecryptedByConfiguredLegacyKey() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        String legacyCiphertext = encryptLegacyV1("alice@example.test", "real-legacy-key");
        PiiCryptoConverter.configure("", "", "wrong-legacy-key");
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq("enc:v1:%")))
                .thenReturn(1L, 0L);
        when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq("enc:v1:%")))
                .thenReturn(legacyCiphertext);

        PiiLegacyCiphertextStartupGuard guard = new PiiLegacyCiphertextStartupGuard(jdbcTemplate, true);

        assertThatThrownBy(() -> guard.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("cannot decrypt legacy enc:v1 rows")
                .hasMessageContaining("users.phone");
    }

    @Test
    void acceptsLegacyRowsWhenConfiguredLegacyKeyCanDecryptSamples() throws Exception {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        String legacyKey = "real-legacy-key";
        String legacyCiphertext = encryptLegacyV1("alice@example.test", legacyKey);
        PiiCryptoConverter.configure("", "", legacyKey);
        when(jdbcTemplate.queryForObject(any(String.class), eq(Long.class), eq("enc:v1:%")))
                .thenReturn(1L, 0L);
        when(jdbcTemplate.queryForObject(any(String.class), eq(String.class), eq("enc:v1:%")))
                .thenReturn(legacyCiphertext);

        new PiiLegacyCiphertextStartupGuard(jdbcTemplate, true).run(null);
    }

    private static String encryptLegacyV1(String value, String rawKey) throws Exception {
        byte[] key = MessageDigest.getInstance("SHA-256")
                .digest(rawKey.trim().getBytes(StandardCharsets.UTF_8));
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(128, iv));
        byte[] encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        byte[] payload = new byte[iv.length + encrypted.length];
        System.arraycopy(iv, 0, payload, 0, iv.length);
        System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
        return "enc:v1:" + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
    }
}
