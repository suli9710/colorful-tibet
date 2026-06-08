package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class PiiCryptoConverterTest {

    private static final String KEY_32_BYTES_BASE64 =
            Base64.getEncoder().encodeToString("0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8));

    @AfterEach
    void resetKeys() {
        PiiCryptoConverter.configure("", "", "");
        PiiCryptoConverter.configureMetrics(null);
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
    void ordinaryPlaintextStillReadsAndBackfillsAsPlaintext() {
        PiiCryptoConverter.configure("kid1:" + KEY_32_BYTES_BASE64, "kid1", "");
        PiiCryptoConverter converter = new PiiCryptoConverter();

        PiiCryptoConverter.BackfillValue backfillValue = converter.valueForBackfill("13900000000");

        assertThat(converter.convertToEntityAttribute("13900000000")).isEqualTo("13900000000");
        assertThat(backfillValue.encryptable()).isTrue();
        assertThat(backfillValue.plaintext()).isEqualTo("13900000000");
    }

    @Test
    void malformedEncryptedPrefixesFailClosedInsteadOfReturningCiphertext() {
        PiiCryptoConverter.configure("kid1:" + KEY_32_BYTES_BASE64, "kid1", "");
        PiiCryptoConverter converter = new PiiCryptoConverter();

        assertThatThrownBy(() -> converter.convertToEntityAttribute("enc:v2:kid1:not-a-real-ciphertext"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Failed to decrypt encrypted PII field")
                .hasMessageContaining("version=v2")
                .hasMessageContaining("reason=malformed_payload")
                .hasMessageNotContaining("not-a-real-ciphertext");
        assertThatThrownBy(() -> converter.convertToEntityAttribute("enc:v2:missing-kid:not-a-real-ciphertext"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("version=v2")
                .hasMessageContaining("reason=missing_key")
                .hasMessageNotContaining("not-a-real-ciphertext");
        assertThatThrownBy(() -> converter.convertToEntityAttribute("enc:v1:not-a-real-ciphertext"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("version=v1")
                .hasMessageContaining("reason=missing_key")
                .hasMessageNotContaining("not-a-real-ciphertext");
    }

    @Test
    void decryptFailuresEmitMetricsAndStructuredSecurityLogsWithoutCiphertext() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        PiiCryptoConverter.configure("kid1:" + KEY_32_BYTES_BASE64, "kid1", "");
        PiiCryptoConverter.configureMetrics(meterRegistry);
        PiiCryptoConverter converter = new PiiCryptoConverter();
        ListAppender<ILoggingEvent> appender = attachAppender();
        String ciphertext = "enc:v2:missing-kid:not-a-real-ciphertext";

        try {
            assertThatThrownBy(() -> converter.convertToEntityAttribute(ciphertext))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to decrypt encrypted PII field")
                    .hasMessageContaining("version=v2")
                    .hasMessageContaining("reason=missing_key")
                    .hasMessageNotContaining(ciphertext)
                    .hasMessageNotContaining("not-a-real-ciphertext");

            assertThat(meterRegistry.find("pii_decrypt_failure_total")
                    .tag("version", "v2")
                    .tag("reason", "missing_key")
                    .counter()
                    .count()).isEqualTo(1.0);
            assertThat(appender.list).hasSize(1);
            ILoggingEvent event = appender.list.get(0);
            assertThat(event.getFormattedMessage()).isEqualTo("PII decrypt failure");
            assertThat(event.getKeyValuePairs()).anySatisfy(pair -> {
                assertThat(pair.key).isEqualTo("security_event");
                assertThat(pair.value).isEqualTo("pii_decrypt_failure");
            });
            assertThat(event.getKeyValuePairs()).anySatisfy(pair -> {
                assertThat(pair.key).isEqualTo("version");
                assertThat(pair.value).isEqualTo("v2");
            });
            assertThat(event.getKeyValuePairs()).anySatisfy(pair -> {
                assertThat(pair.key).isEqualTo("reason");
                assertThat(pair.value).isEqualTo("missing_key");
            });
            assertThat(event.getFormattedMessage() + event.getKeyValuePairs()).doesNotContain(ciphertext);
            assertThat(event.getFormattedMessage() + event.getKeyValuePairs()).doesNotContain("not-a-real-ciphertext");
        } finally {
            detachAppender(appender);
        }
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

    private ListAppender<ILoggingEvent> attachAppender() {
        Logger logger = (Logger) LoggerFactory.getLogger(PiiCryptoConverter.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        return appender;
    }

    private void detachAppender(ListAppender<ILoggingEvent> appender) {
        Logger logger = (Logger) LoggerFactory.getLogger(PiiCryptoConverter.class);
        logger.detachAppender(appender);
    }
}
