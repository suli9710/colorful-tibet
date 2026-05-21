package com.tibet.tourism.common.security;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Converter
public class PiiCryptoConverter implements AttributeConverter<String, String> {

    private static final String PREFIX = "enc:v1:";
    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static volatile byte[] encryptionKey;

    static void configure(String rawKey) {
        if (!StringUtils.hasText(rawKey)) {
            encryptionKey = null;
            return;
        }
        try {
            encryptionKey = MessageDigest.getInstance("SHA-256")
                    .digest(rawKey.trim().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize PII encryption key", e);
        }
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (!StringUtils.hasText(attribute) || attribute.startsWith(PREFIX)) {
            return attribute;
        }
        byte[] key = requireKey();
        try {
            byte[] iv = new byte[IV_BYTES];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt PII field", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (!StringUtils.hasText(dbData) || !dbData.startsWith(PREFIX)) {
            return dbData;
        }
        byte[] key = requireKey();
        try {
            byte[] payload = Base64.getUrlDecoder().decode(dbData.substring(PREFIX.length()));
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_BYTES);
            byte[] encrypted = Arrays.copyOfRange(payload, IV_BYTES, payload.length);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt PII field", e);
        }
    }

    private byte[] requireKey() {
        byte[] key = encryptionKey;
        if (key == null) {
            throw new IllegalStateException("PII_ENCRYPTION_KEY must be configured before writing encrypted PII");
        }
        return key;
    }

    @Component
    static class KeyInitializer {
        KeyInitializer(@Value("${app.security.pii-encryption-key:}") String rawKey) {
            PiiCryptoConverter.configure(rawKey);
        }
    }
}
