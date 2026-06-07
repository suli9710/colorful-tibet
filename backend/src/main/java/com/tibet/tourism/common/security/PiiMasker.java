package com.tibet.tourism.common.security;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import org.springframework.util.StringUtils;

public final class PiiMasker {

    private static final int SHORT_HASH_BYTES = 6;

    private PiiMasker() {
    }

    public static String maskPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return phone;
        }
        String normalized = phone.trim();
        String digits = normalized.replaceAll("\\D", "");
        if (digits.length() >= 7) {
            return digits.substring(0, 3) + "****" + digits.substring(digits.length() - 4);
        }
        if (normalized.length() <= 2) {
            return "*".repeat(normalized.length());
        }
        return normalized.charAt(0) + "***" + normalized.charAt(normalized.length() - 1);
    }

    public static String maskName(String name) {
        if (!StringUtils.hasText(name)) {
            return name;
        }
        String normalized = name.trim();
        if (normalized.length() == 1) {
            return "*";
        }
        if (normalized.length() == 2) {
            return normalized.charAt(0) + "*";
        }
        return normalized.charAt(0) + "***" + normalized.charAt(normalized.length() - 1);
    }

    public static String maskIp(String ipAddress) {
        if (!StringUtils.hasText(ipAddress)) {
            return "ip#empty";
        }
        return "ip#" + shortHash(ipAddress.trim());
    }

    public static String shortHash(String value) {
        if (!StringUtils.hasText(value)) {
            return "empty";
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(value.trim().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed, 0, SHORT_HASH_BYTES);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash value for log label", e);
        }
    }
}
