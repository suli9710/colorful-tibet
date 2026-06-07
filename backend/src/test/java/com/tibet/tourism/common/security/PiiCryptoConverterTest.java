package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PiiCryptoConverterTest {

    private static final String KEY_32_BYTES_BASE64 =
            Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    @AfterEach
    void resetKeys() {
        PiiCryptoConverter.configure("", "", "");
    }

    @Test
    void encryptsNewValuesAsV2WithActiveKidAndDecryptsThem() {
        PiiCryptoConverter.configure("kid1:" + KEY_32_BYTES_BASE64, "kid1", "");
        PiiCryptoConverter converter = new PiiCryptoConverter();

        String encrypted = converter.convertToDatabaseColumn("13900000000");

        assertThat(encrypted).startsWith("enc:v2:kid1:");
        assertThat(converter.convertToEntityAttribute(encrypted)).isEqualTo("13900000000");
    }

    @Test
    void encryptsPrefixLookingPlaintextInsteadOfTrustingIt() {
        PiiCryptoConverter.configure("kid1:" + KEY_32_BYTES_BASE64, "kid1", "");
        PiiCryptoConverter converter = new PiiCryptoConverter();
        String forgedPrefixValue = "enc:v2:kid1:not-a-real-ciphertext";

        String encrypted = converter.convertToDatabaseColumn(forgedPrefixValue);

        assertThat(encrypted).startsWith("enc:v2:kid1:");
        assertThat(encrypted).isNotEqualTo(forgedPrefixValue);
        assertThat(converter.convertToEntityAttribute(encrypted)).isEqualTo(forgedPrefixValue);
    }

    @Test
    void malformedEncryptedPrefixesReadAsPlaintextInsteadOfThrowing() {
        PiiCryptoConverter.configure("kid1:" + KEY_32_BYTES_BASE64, "kid1", "");
        PiiCryptoConverter converter = new PiiCryptoConverter();

        assertThat(converter.convertToEntityAttribute("enc:v2:kid1:not-a-real-ciphertext"))
                .isEqualTo("enc:v2:kid1:not-a-real-ciphertext");
        assertThat(converter.convertToEntityAttribute("enc:v2:missing-kid:not-a-real-ciphertext"))
                .isEqualTo("enc:v2:missing-kid:not-a-real-ciphertext");
        assertThat(converter.convertToEntityAttribute("enc:v1:not-a-real-ciphertext"))
                .isEqualTo("enc:v1:not-a-real-ciphertext");
    }

    @Test
    void rejectsNonBase64OrWrongLengthV2KeysAtStartupConfiguration() {
        String shortKey = Base64.getEncoder().encodeToString("too-short".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> PiiCryptoConverter.configure("kid1:" + shortKey, "kid1", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32 random bytes");
    }

    @Test
    void rejectsPlaceholderV2KeysAtStartupConfiguration() {
        assertThatThrownBy(() -> PiiCryptoConverter.configure("kid1:change-me", "kid1", ""))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("placeholder");
    }

    @Test
    void readsLegacyV1PayloadWhenLegacyKeyIsConfiguredButWritesOnlyV2() throws Exception {
        String legacyRawKey = "legacy-strong-enough-test-key";
        String v1 = encryptLegacyV1("alice@example.test", legacyRawKey);
        PiiCryptoConverter.configure("kid2:" + KEY_32_BYTES_BASE64, "kid2", legacyRawKey);
        PiiCryptoConverter converter = new PiiCryptoConverter();

        String plaintext = converter.convertToEntityAttribute(v1);
        String rewritten = converter.convertToDatabaseColumn(plaintext);

        assertThat(plaintext).isEqualTo("alice@example.test");
        assertThat(rewritten).startsWith("enc:v2:kid2:");
        assertThat(converter.convertToEntityAttribute(rewritten)).isEqualTo("alice@example.test");
    }

    private String encryptLegacyV1(String value, String rawKey) throws Exception {
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
