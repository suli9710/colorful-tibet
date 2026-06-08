package com.tibet.tourism.modules.recommendation.application;

import com.tibet.tourism.common.security.SensitiveLogSanitizer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Collection;
import java.util.HexFormat;

public final class RecommendationLogPrivacy {

    private static final int USER_REF_HEX_LENGTH = 12;
    private static final byte[] LOG_HASH_SALT = new byte[16];

    static {
        new SecureRandom().nextBytes(LOG_HASH_SALT);
    }

    private RecommendationLogPrivacy() {
    }

    public static String userRef(Long userId) {
        if (userId == null) {
            return "user:unknown";
        }
        return "user:" + hash(Long.toString(userId));
    }

    public static int collectionSize(Collection<?> values) {
        return values == null ? 0 : values.size();
    }

    public static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    public static boolean locationProvided(Double latitude, Double longitude) {
        return latitude != null && longitude != null;
    }

    public static Double roundedDistanceKm(Double distanceKm) {
        if (distanceKm == null) {
            return null;
        }
        return Math.round(distanceKm * 10.0) / 10.0;
    }

    public static int textLength(String value) {
        return value == null ? 0 : value.length();
    }

    public static String exceptionSummary(Throwable exception) {
        return SensitiveLogSanitizer.exceptionSummary(exception);
    }

    private static String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(LOG_HASH_SALT);
            String hex = HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
            return hex.substring(0, USER_REF_HEX_LENGTH);
        } catch (NoSuchAlgorithmException ex) {
            return "redacted";
        }
    }
}
