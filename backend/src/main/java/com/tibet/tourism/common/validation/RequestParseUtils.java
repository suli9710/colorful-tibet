package com.tibet.tourism.common.validation;
import java.math.BigDecimal;

public final class RequestParseUtils {

    private RequestParseUtils() {}

    public static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    public static String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    public static String optionalStringValue(Object value) {
        String string = stringValue(value);
        return isBlank(string) ? null : string.trim();
    }

    public static String safeImageUrl(Object value, String fieldName) {
        return InputSanitizer.optionalPublicImageUrl(stringValue(value), fieldName);
    }

    public static Integer integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string && !string.trim().isEmpty()) {
            try {
                return Integer.parseInt(string.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    public static Boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String string) {
            return Boolean.parseBoolean(string);
        }
        return false;
    }

    public static BigDecimal decimalValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        String string = value.toString();
        if (isBlank(string)) {
            return null;
        }
        try {
            return new BigDecimal(string.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static String firstStringValue(java.util.Map<String, Object> request, String firstKey, String secondKey) {
        if (request.containsKey(firstKey)) {
            return stringValue(request.get(firstKey));
        }
        if (request.containsKey(secondKey)) {
            return stringValue(request.get(secondKey));
        }
        return null;
    }
}
