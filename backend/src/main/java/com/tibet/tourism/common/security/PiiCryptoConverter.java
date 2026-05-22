package com.tibet.tourism.common.security;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Converter
public class PiiCryptoConverter implements AttributeConverter<String, String> {

    private static final String PREFIX_V1 = "enc:v1:";
    private static final String PREFIX_V2 = "enc:v2:";
    private static final String CIPHER = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int KEY_BYTES = 32;
    private static final Pattern KEY_ID = Pattern.compile("^[A-Za-z0-9._-]{1,64}$");
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static volatile Map<String, byte[]> encryptionKeys = Map.of();
    private static volatile String activeKid;
    private static volatile byte[] legacyV1Key;

    static void configure(String rawKeys, String rawActiveKid, String legacyRawKey) {
        Map<String, byte[]> parsedKeys = parseKeys(rawKeys);
        String parsedActiveKid = normalizeActiveKid(rawActiveKid, parsedKeys);
        byte[] parsedLegacyKey = parseLegacyKey(legacyRawKey);

        encryptionKeys = parsedKeys;
        activeKid = parsedActiveKid;
        legacyV1Key = parsedLegacyKey;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (!StringUtils.hasText(attribute)
                || attribute.startsWith(PREFIX_V2)
                || attribute.startsWith(PREFIX_V1)) {
            return attribute;
        }
        String kid = requireActiveKid();
        byte[] key = requireKey(kid);
        try {
            byte[] iv = new byte[IV_BYTES];
            SECURE_RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(attribute.getBytes(StandardCharsets.UTF_8));
            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return PREFIX_V2 + kid + ":" + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to encrypt PII field", e);
        }
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (!StringUtils.hasText(dbData)) {
            return dbData;
        }
        if (dbData.startsWith(PREFIX_V2)) {
            return decryptV2(dbData);
        }
        if (dbData.startsWith(PREFIX_V1)) {
            return decryptV1(dbData);
        }
        return dbData;
    }

    private String decryptV2(String dbData) {
        int kidStart = PREFIX_V2.length();
        int separator = dbData.indexOf(':', kidStart);
        if (separator <= kidStart || separator == dbData.length() - 1) {
            throw new IllegalStateException("Invalid encrypted PII v2 payload");
        }
        String kid = dbData.substring(kidStart, separator);
        byte[] key = requireKey(kid);
        return decryptPayload(dbData.substring(separator + 1), key, "v2");
    }

    private String decryptV1(String dbData) {
        byte[] key = legacyV1Key;
        if (key == null) {
            throw new IllegalStateException("PII_ENCRYPTION_KEY must be configured to read legacy encrypted PII");
        }
        return decryptPayload(dbData.substring(PREFIX_V1.length()), key, "v1");
    }

    private String decryptPayload(String encodedPayload, byte[] key, String version) {
        try {
            byte[] payload = Base64.getUrlDecoder().decode(encodedPayload);
            if (payload.length <= IV_BYTES) {
                throw new IllegalStateException("PII payload is too short");
            }
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_BYTES);
            byte[] encrypted = Arrays.copyOfRange(payload, IV_BYTES, payload.length);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(key, "AES"), new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to decrypt PII field " + version, e);
        }
    }

    private static String requireActiveKid() {
        String kid = activeKid;
        if (!StringUtils.hasText(kid)) {
            throw new IllegalStateException("PII_KEYS and PII_ACTIVE_KID must be configured before writing encrypted PII");
        }
        return kid;
    }

    private static byte[] requireKey(String kid) {
        byte[] key = encryptionKeys.get(kid);
        if (key == null) {
            throw new IllegalStateException("Unknown PII encryption key id: " + kid);
        }
        return key;
    }

    private static Map<String, byte[]> parseKeys(String rawKeys) {
        if (!StringUtils.hasText(rawKeys)) {
            return Map.of();
        }
        Map<String, byte[]> parsed = new LinkedHashMap<>();
        for (String entry : rawKeys.split(",")) {
            if (!StringUtils.hasText(entry)) {
                continue;
            }
            int separator = entry.indexOf(':');
            if (separator <= 0 || separator == entry.length() - 1) {
                throw new IllegalStateException("PII_KEYS entries must use kid:base64-32-byte-key format");
            }
            String kid = entry.substring(0, separator).trim();
            if (!KEY_ID.matcher(kid).matches()) {
                throw new IllegalStateException("PII key id contains invalid characters");
            }
            if (parsed.containsKey(kid)) {
                throw new IllegalStateException("PII key ids must be unique");
            }
            parsed.put(kid, decodeStrictKey(entry.substring(separator + 1).trim(), "PII key"));
        }
        return Map.copyOf(parsed);
    }

    private static String normalizeActiveKid(String rawActiveKid, Map<String, byte[]> parsedKeys) {
        if (parsedKeys.isEmpty()) {
            return null;
        }
        String kid = rawActiveKid == null ? "" : rawActiveKid.trim();
        if (!StringUtils.hasText(kid)) {
            throw new IllegalStateException("PII_ACTIVE_KID must be configured when PII_KEYS is set");
        }
        if (!parsedKeys.containsKey(kid)) {
            throw new IllegalStateException("PII_ACTIVE_KID must match an entry in PII_KEYS");
        }
        return kid;
    }

    private static byte[] parseLegacyKey(String legacyRawKey) {
        if (!StringUtils.hasText(legacyRawKey)) {
            return null;
        }
        try {
            return MessageDigest.getInstance("SHA-256")
                    .digest(legacyRawKey.trim().getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize legacy PII encryption key", e);
        }
    }

    private static byte[] decodeStrictKey(String rawKey, String label) {
        rejectPlaceholder(rawKey, label);
        try {
            byte[] decoded = Base64.getDecoder().decode(rawKey);
            if (decoded.length != KEY_BYTES) {
                throw new IllegalStateException(label + " must be exactly 32 random bytes");
            }
            return decoded;
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(label + " must be valid Base64", exception);
        }
    }

    private static void rejectPlaceholder(String rawKey, String label) {
        String normalized = rawKey == null ? "" : rawKey.trim().toLowerCase(Locale.ROOT);
        if (!StringUtils.hasText(normalized)
                || normalized.contains("change-me")
                || normalized.contains("changeme")
                || normalized.contains("replace-with")
                || normalized.contains("placeholder")) {
            throw new IllegalStateException(label + " must not be blank or a placeholder");
        }
    }

    @Component
    static class KeyInitializer {
        KeyInitializer(
                @Value("${app.security.pii-keys:${PII_KEYS:}}") String rawKeys,
                @Value("${app.security.pii-active-kid:${PII_ACTIVE_KID:}}") String activeKid,
                @Value("${app.security.pii-encryption-key:${PII_ENCRYPTION_KEY:}}") String legacyRawKey) {
            PiiCryptoConverter.configure(rawKeys, activeKid, legacyRawKey);
        }
    }
}
