package com.tibet.tourism.common.validation;
import com.tibet.tourism.modules.user.domain.User;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.Collection;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

public final class InputSanitizer {

    private static final Pattern CONTROL_OR_FORMAT_CHARS = Pattern.compile("[\\p{Cntrl}&&[^\r\n\t]]|\\p{Cf}");
    private static final Pattern MULTIPLE_SPACES = Pattern.compile("[\\s&&[^\r\n]]+");
    private static final Pattern TAG_SEPARATOR = Pattern.compile("[,，;；\\s]+");
    private static final Pattern SAFE_TAG = Pattern.compile("^[\\p{L}\\p{M}\\p{N}#-]{1,24}$");
    private static final Pattern SAFE_LOCAL_ASSET_PATH =
            Pattern.compile("^/(uploads|images)/(?:[\\p{L}\\p{M}\\p{N}_-]+/)*[\\p{L}\\p{M}\\p{N}_-][\\p{L}\\p{M}\\p{N}._-]*$");
    private static final Pattern SAFE_APP_LINK_PATH =
            Pattern.compile("^/[\\p{L}\\p{M}\\p{N}][\\p{L}\\p{M}\\p{N}/._~?#=&%+-]{0,511}$");

    private static final int MIN_PASSWORD_LENGTH = 8;
    private static final int MAX_PASSWORD_LENGTH = 72;
    private static final Pattern PASSWORD_HAS_LOWER = Pattern.compile("[a-z]");
    private static final Pattern PASSWORD_HAS_UPPER = Pattern.compile("[A-Z]");
    private static final Pattern PASSWORD_HAS_DIGIT = Pattern.compile("\\d");
    private static final Pattern PASSWORD_HAS_SPECIAL = Pattern.compile("[^A-Za-z0-9]");
    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password", "password1", "password123", "qwerty123", "qwertyuiop",
            "admin123", "admin123456", "letmein123", "welcome1", "abc123456",
            "changeme", "changeit", "iloveyou", "12345678", "123456789",
            "p@ssw0rd", "passw0rd", "colorfultibet", "tibet123");
    private static final Pattern PROMPT_CONTROL_TOKENS = Pattern.compile(
            "(?i)(ignore\\s+(all\\s+)?previous|system\\s*prompt|developer\\s*message|assistant\\s*:|user\\s*:|system\\s*:|###|```)");

    private InputSanitizer() {
    }

    public static void validatePassword(String password) {
        if (password == null || password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            throw new IllegalArgumentException("密码长度需为" + MIN_PASSWORD_LENGTH + "到" + MAX_PASSWORD_LENGTH + "个字符");
        }
        if (!PASSWORD_HAS_LOWER.matcher(password).find()) {
            throw new IllegalArgumentException("密码必须包含至少一个小写字母");
        }
        if (!PASSWORD_HAS_UPPER.matcher(password).find()) {
            throw new IllegalArgumentException("密码必须包含至少一个大写字母");
        }
        if (!PASSWORD_HAS_DIGIT.matcher(password).find()) {
            throw new IllegalArgumentException("密码必须包含至少一个数字");
        }
        if (!PASSWORD_HAS_SPECIAL.matcher(password).find()) {
            throw new IllegalArgumentException("密码必须包含至少一个特殊字符");
        }
        if (COMMON_PASSWORDS.contains(password.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("密码过于常见，请更换更强的密码");
        }
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
        if (containsTraversal(normalized) || containsEncodedOrDelimitedPath(normalized)
                || normalized.contains("\\") || normalized.contains("//")
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

    public static String optionalSafeLinkUrl(String value, String fieldName) {
        String normalized = normalizeSingleLine(value);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        if (normalized.length() > 512 || normalized.contains("\\") || containsTraversal(normalized)) {
            throw new IllegalArgumentException(fieldName + "不合法");
        }
        if (normalized.startsWith("/")) {
            if (normalized.startsWith("//") || containsEncodedOrDelimitedPathForAppLink(normalized)
                    || !SAFE_APP_LINK_PATH.matcher(normalized).matches()) {
                throw new IllegalArgumentException(fieldName + "只能使用站内路径或HTTPS地址");
            }
            return normalized;
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
            throw new IllegalArgumentException(fieldName + "只能使用站内路径或HTTPS地址");
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
        String strippedControls = PROMPT_CONTROL_TOKENS.matcher(clipped).replaceAll("[filtered]");
        return HtmlUtils.htmlEscape(strippedControls);
    }

    public static String sha256HexForStorage(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.trim().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to hash value for storage", e);
        }
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

    private static boolean containsEncodedOrDelimitedPath(String value) {
        return value.indexOf('%') >= 0
                || value.indexOf(';') >= 0
                || value.indexOf('?') >= 0
                || value.indexOf('#') >= 0;
    }

    private static boolean containsEncodedOrDelimitedPathForAppLink(String value) {
        return value.indexOf(';') >= 0
                || value.toLowerCase(Locale.ROOT).contains("%2f")
                || value.toLowerCase(Locale.ROOT).contains("%5c");
    }
}
