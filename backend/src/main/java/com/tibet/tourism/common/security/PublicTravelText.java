package com.tibet.tourism.common.security;

import java.util.regex.Pattern;

/** Data minimization for public travel prose, not a guarantee that free text is anonymous. */
public final class PublicTravelText {
    private static final Pattern ID = Pattern.compile("(?<![A-Za-z0-9])\\d{17}[0-9Xx](?![A-Za-z0-9])");
    private static final Pattern PHONE = Pattern.compile("(?<![A-Za-z0-9])(?:\\+?86[- ]?)?1[3-9]\\d{9}(?![A-Za-z0-9])");
    private static final Pattern EMAIL = Pattern.compile("(?i)(?<![A-Za-z0-9._%+-])[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}(?![A-Z0-9])");
    private static final Pattern PRIVATE_FIELD = Pattern.compile(
            "(?im)(?:^|(?<=\\n))[\\t ]*(?:[-*][\\t ]*)?(?:身份证(?:号|号码)?|护照(?:号|号码)?|证件号|手机号|联系电话|病史|健康史|用药|医疗记录|passport|medical history|medications)\\s*[:：]\\s*[^\\r\\n]*");

    private PublicTravelText() {}

    public static String minimize(String value) {
        if (value == null || value.isEmpty()) return value;
        String text = PRIVATE_FIELD.matcher(value).replaceAll("[私人资料已隐藏]");
        text = ID.matcher(text).replaceAll("[证件号码已隐藏]");
        text = PHONE.matcher(text).replaceAll("[手机号码已隐藏]");
        return EMAIL.matcher(text).replaceAll("[邮箱已隐藏]");
    }
}
