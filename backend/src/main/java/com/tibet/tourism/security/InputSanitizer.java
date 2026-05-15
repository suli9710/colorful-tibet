package com.tibet.tourism.security;

import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class InputSanitizer {

    private static final Pattern CONTROL_OR_FORMAT_CHARS = Pattern.compile("[\\p{Cntrl}&&[^\r\n\t]]|\\p{Cf}");
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("[\\s&&[^\r\n]]+");
    private static final Pattern TAG_SEPARATOR = Pattern.compile("[,，;；\\s]+");
    private static final Pattern SAFE_TAG = Pattern.compile("^[\\p{L}\\p{M}\\p{N}#-]{1,24}$");
    private static final Pattern SAFE_LOCAL_ASSET_PATH =
            Pattern.compile("^/(uploads|images)/[A-Za-z0-9._~!$&'()*+,;=:@%/-]+$");

    private InputSanitizer() {
    }

    public static String requiredPlainText(String value, int maxLength, String fieldName) {
        String normalized = normalizeSingleLine(value);
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return escapeAndValidateLength(normalized, maxLength, fieldName);
    }

    public static String optionalPlainText(String value, int maxLength, String fieldName) {
        String normalized = normalizeSingleLine(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return escapeAndValidateLength(normalized, maxLength, fieldName);
    }

    public static String requiredTextBlock(String value, int maxLength, String fieldName) {
        String normalized = normalizeTextBlock(value);
        if (!StringUtils.hasText(normalized)) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return escapeAndValidateLength(normalized, maxLength, fieldName);
    }

    public static String optionalTextBlock(String value, int maxLength, String fieldName) {
        String normalized = normalizeTextBlock(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return escapeAndValidateLength(normalized, maxLength, fieldName);
    }

    public static String optionalAllowedValue(String value, Set<String> allowedValues, String fieldName) {
        String normalized = normalizeSingleLine(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (!allowedValues.contains(normalized)) {
            throw new IllegalArgumentException(fieldName + "不合法");
        }
        return normalized;
    }

    public static String optionalTags(String value, int maxTotalLength) {
        String normalized = normalizeSingleLine(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }

        Set<String> tags = Arrays.stream(TAG_SEPARATOR.split(normalized))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .limit(10)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (tags.isEmpty()) {
            return null;
        }

        for (String tag : tags) {
            if (!SAFE_TAG.matcher(tag).matches()) {
                throw new IllegalArgumentException("标签只能包含文字、数字、#或短横线");
            }
        }

        String joinedTags = String.join(",", tags);
        if (joinedTags.length() > maxTotalLength) {
            throw new IllegalArgumentException("标签长度不能超过" + maxTotalLength + "个字符");
        }
        return joinedTags;
    }

    public static String optionalTagFilter(String value) {
        String normalized = normalizeSingleLine(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (!SAFE_TAG.matcher(normalized).matches()) {
            throw new IllegalArgumentException("标签筛选条件不合法");
        }
        return normalized;
    }

    public static String optionalLocalAssetPath(String value, String fieldName) {
        String normalized = normalizeSingleLine(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (normalized.length() > 512) {
            throw new IllegalArgumentException(fieldName + "长度不能超过512个字符");
        }
        if (containsTraversal(normalized) || normalized.contains("\\")
                || !SAFE_LOCAL_ASSET_PATH.matcher(normalized).matches()) {
            throw new IllegalArgumentException(fieldName + "只能使用站内上传资源路径");
        }
        return normalized;
    }

    public static String optionalPublicImageUrl(String value, String fieldName) {
        String normalized = normalizeSingleLine(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (normalized.startsWith("/")) {
            return optionalLocalAssetPath(normalized, fieldName);
        }
        if (normalized.length() > 512) {
            throw new IllegalArgumentException(fieldName + "长度不能超过512个字符");
        }
        if (normalized.contains("\\") || containsTraversal(normalized)) {
            throw new IllegalArgumentException(fieldName + "不合法");
        }

        URI uri;
        try {
            uri = new URI(normalized);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(fieldName + "不合法");
        }

        if (!"https".equalsIgnoreCase(uri.getScheme())
                || !StringUtils.hasText(uri.getHost())
                || StringUtils.hasText(uri.getUserInfo())) {
            throw new IllegalArgumentException(fieldName + "必须是HTTPS地址或站内资源路径");
        }
        return normalized;
    }

    public static String safeSortField(String value, Collection<String> allowedFields, String defaultField) {
        String normalized = normalizeSingleLine(value);
        if (!StringUtils.hasText(normalized)) {
            return defaultField;
        }
        return allowedFields.contains(normalized) ? normalized : defaultField;
    }

    public static int normalizePage(int page) {
        return Math.max(0, page);
    }

    public static int normalizePageSize(int size, int defaultSize, int maxSize) {
        if (size <= 0) {
            return defaultSize;
        }
        return Math.min(size, maxSize);
    }

    public static String promptData(String value, int maxLength) {
        String normalized = normalizeSingleLine(value);
        if (!StringUtils.hasText(normalized)) {
            return "";
        }
        String clipped = normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
        return HtmlUtils.htmlEscape(clipped);
    }

    private static String escapeAndValidateLength(String value, int maxLength, String fieldName) {
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + "长度不能超过" + maxLength + "个字符");
        }
        String escaped = HtmlUtils.htmlEscape(value);
        if (escaped.length() > maxLength) {
            throw new IllegalArgumentException(fieldName + "包含过多特殊字符");
        }
        return escaped;
    }

    private static String normalizeSingleLine(String value) {
        if (value == null) {
            return "";
        }
        String normalized = normalize(value)
                .replace('\r', ' ')
                .replace('\n', ' ')
                .replace('\t', ' ');
        return MULTIPLE_SPACES.matcher(normalized).replaceAll(" ").trim();
    }

    private static String normalizeTextBlock(String value) {
        if (value == null) {
            return "";
        }
        return normalize(value)
                .replace("\r\n", "\n")
                .replace('\r', '\n')
                .trim();
    }

    private static String normalize(String value) {
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC);
        return CONTROL_OR_FORMAT_CHARS.matcher(normalized).replaceAll("");
    }

    private static boolean containsTraversal(String value) {
        String lower = value.toLowerCase();
        return lower.contains("..") || lower.contains("%2e");
    }
}
