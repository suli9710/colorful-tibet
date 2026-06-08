package com.tibet.tourism.common.security;

import org.springframework.util.StringUtils;

public final class SensitiveLogSanitizer {

    private SensitiveLogSanitizer() {
    }

    public static String exceptionSummary(Throwable throwable) {
        if (throwable == null) {
            return "type=unknown,messageHash=empty";
        }
        Throwable root = rootCause(throwable);
        String summary = "type=" + throwable.getClass().getSimpleName();
        if (root != null && root.getClass() != throwable.getClass()) {
            summary += ",rootType=" + root.getClass().getSimpleName();
        }
        return summary + ",messageHash=" + messageHash(root);
    }

    private static Throwable rootCause(Throwable throwable) {
        Throwable current = throwable;
        Throwable root = throwable;
        while (current != null) {
            root = current;
            current = current.getCause();
        }
        return root;
    }

    private static String messageHash(Throwable throwable) {
        if (throwable == null || !StringUtils.hasText(throwable.getMessage())) {
            return "empty";
        }
        return PiiMasker.shortHash(throwable.getMessage());
    }
}
