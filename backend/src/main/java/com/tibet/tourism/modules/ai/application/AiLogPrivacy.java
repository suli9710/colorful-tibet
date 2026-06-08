package com.tibet.tourism.modules.ai.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

final class AiLogPrivacy {

    private static final int USER_REF_HEX_LENGTH = 12;
    private static final byte[] LOG_HASH_SALT = new byte[16];

    static {
        new SecureRandom().nextBytes(LOG_HASH_SALT);
    }

    private AiLogPrivacy() {
    }

    static String userRef(Long userId) {
        if (userId == null) {
            return "user:unknown";
        }
        return "user:" + hash(Long.toString(userId));
    }

    static String userRef(long userId) {
        return userRef(Long.valueOf(userId));
    }

    static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    static String exceptionSummary(Throwable exception) {
        if (exception == null) {
            return "unknown";
        }
        return exception.getClass().getSimpleName();
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
