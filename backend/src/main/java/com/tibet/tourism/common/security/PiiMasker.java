package com.tibet.tourism.common.security;
import org.springframework.util.StringUtils;

public final class PiiMasker {

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
}
