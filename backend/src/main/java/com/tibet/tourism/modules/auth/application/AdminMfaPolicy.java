package com.tibet.tourism.modules.auth.application;

import com.tibet.tourism.common.security.CacheKeyHasher;
import com.tibet.tourism.modules.user.domain.User;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Resolves the independent TOTP secret assigned to each administrator.
 *
 * <p>The configured super-admin secret remains a dedicated backwards-compatible path. Other
 * administrators are deliberately looked up by their normalized username; there is no shared
 * fallback secret, so an administrator can be rotated or revoked without affecting another one.
 */
@Service
public class AdminMfaPolicy {

    private static final Pattern SAFE_USERNAME = Pattern.compile("^[A-Za-z0-9_-]{3,64}$");
    private static final Pattern OPAQUE_BINDING = Pattern.compile("^[0-9a-f]{32}$");
    private static final String BINDING_NAMESPACE = "admin-mfa-binding-v1";

    public enum Verification {
        NOT_REQUIRED,
        REQUIRED,
        INVALID,
        UNCONFIGURED,
        VERIFIED
    }

    private final TotpService totpService;
    private final CacheKeyHasher cacheKeyHasher;
    private final String superAdminUsername;
    private final String superAdminTotpSecret;
    private final boolean requireStrongSecrets;
    private final Environment environment;
    private final Map<String, String> adminTotpSecrets;

    public AdminMfaPolicy(
            TotpService totpService,
            CacheKeyHasher cacheKeyHasher,
            @Value("${app.super-admin-username:}") String superAdminUsername,
            @Value("${app.security.super-admin-totp-secret:}") String superAdminTotpSecret,
            @Value("${app.security.admin-totp-secrets:}") String adminTotpSecrets,
            @Value("${app.security.require-strong-secrets:false}") boolean requireStrongSecrets,
            Environment environment) {
        this.totpService = totpService;
        this.cacheKeyHasher = cacheKeyHasher;
        this.superAdminUsername = normalize(superAdminUsername);
        this.superAdminTotpSecret = normalizeSecret(superAdminTotpSecret);
        this.requireStrongSecrets = requireStrongSecrets;
        this.environment = environment;
        this.adminTotpSecrets = parseAdminTotpSecrets(adminTotpSecrets);
        validateConfiguredSecrets();
    }

    public boolean isSuperAdmin(User user) {
        return user != null && isSuperAdminUsername(user.getUsername());
    }

    public boolean isSuperAdminUsername(String username) {
        return StringUtils.hasText(superAdminUsername)
                && superAdminUsername.equals(normalize(username));
    }

    public boolean isProtectedAccount(User user) {
        return user != null && (user.getRole() == User.Role.ADMIN || isSuperAdmin(user));
    }

    public Verification verify(User user, String providedCode) {
        if (!isProtectedAccount(user)) {
            return Verification.NOT_REQUIRED;
        }

        String secret = secretFor(user.getUsername());
        if (!StringUtils.hasText(secret)) {
            return Verification.UNCONFIGURED;
        }
        if (!StringUtils.hasText(providedCode)) {
            return Verification.REQUIRED;
        }
        return totpService.consumeCode(
                        normalize(user.getUsername()), secret, providedCode.trim())
                ? Verification.VERIFIED
                : Verification.INVALID;
    }

    public boolean hasConfiguredSecret(String username) {
        return StringUtils.hasText(secretFor(username));
    }

    public int configuredNamedAdminCount() {
        return adminTotpSecrets.size();
    }

    public boolean hasSuperAdminSecret() {
        return StringUtils.hasText(superAdminTotpSecret);
    }

    /**
     * Returns an opaque, keyed label for the administrator's current MFA credential.
     *
     * <p>The label is safe to place in a signed JWT: it contains neither the username nor TOTP
     * secret, and rotating or removing that administrator's secret changes the expected label.
     */
    public String currentBinding(String username) {
        String normalizedUsername = normalize(username);
        String secret = secretFor(normalizedUsername);
        if (!StringUtils.hasText(normalizedUsername) || !StringUtils.hasText(secret)) {
            return "";
        }

        String label = cacheKeyHasher.cacheKey(
                BINDING_NAMESPACE,
                normalizedUsername + ":" + canonicalSecret(secret));
        int separator = label.lastIndexOf('#');
        String binding = separator >= 0 ? label.substring(separator + 1) : "";
        if (!OPAQUE_BINDING.matcher(binding).matches()) {
            throw new IllegalStateException("Failed to create administrator MFA binding");
        }
        return binding;
    }

    /** Compares the signed JWT label to the currently configured credential in constant time. */
    public boolean matchesCurrentBinding(String username, String presentedBinding) {
        String expectedBinding = currentBinding(username);
        String candidate = presentedBinding == null ? "" : presentedBinding.trim();
        if (!OPAQUE_BINDING.matcher(expectedBinding).matches()
                || !OPAQUE_BINDING.matcher(candidate).matches()) {
            return false;
        }
        return MessageDigest.isEqual(
                expectedBinding.getBytes(StandardCharsets.US_ASCII),
                candidate.getBytes(StandardCharsets.US_ASCII));
    }

    /** Validates startup requirements without exposing any secret material in exception messages. */
    public void validateStartup() {
        boolean strictMode = requireStrongSecrets || isProdProfileActive();
        if (!StringUtils.hasText(superAdminUsername)) {
            if (strictMode) {
                throw new IllegalStateException("Super-admin username must be configured");
            }
            return;
        }

        if (!StringUtils.hasText(superAdminTotpSecret)) {
            if (strictMode) {
                throw new IllegalStateException("Super-admin TOTP secret must be configured");
            }
            return;
        }

        // The constructor has already validated the secret; keeping this method explicit makes the
        // startup contract testable and preserves the previous AuthApplicationService API.
        totpService.validateSecret(superAdminTotpSecret);
    }

    private String secretFor(String username) {
        String normalizedUsername = normalize(username);
        if (isSuperAdminUsername(normalizedUsername)) {
            return superAdminTotpSecret;
        }
        return adminTotpSecrets.get(normalizedUsername);
    }

    private void validateConfiguredSecrets() {
        boolean strictMode = requireStrongSecrets || isProdProfileActive();
        Set<String> independentSecrets = new HashSet<>();
        if (StringUtils.hasText(superAdminTotpSecret)) {
            validateSecret(superAdminTotpSecret, strictMode);
            independentSecrets.add(canonicalSecret(superAdminTotpSecret));
        }
        for (String secret : adminTotpSecrets.values()) {
            validateSecret(secret, strictMode);
            if (!independentSecrets.add(canonicalSecret(secret))) {
                throw new IllegalStateException(
                        "Every administrator must use an independent TOTP secret");
            }
        }
        if (StringUtils.hasText(superAdminUsername)
                && adminTotpSecrets.containsKey(superAdminUsername)) {
            throw new IllegalStateException(
                    "Admin TOTP map must not duplicate the configured super-admin username");
        }
    }

    private void validateSecret(String secret, boolean strictMode) {
        if (strictMode) {
            totpService.validateStrongSecret(secret);
        } else {
            totpService.validateSecret(secret);
        }
    }

    static Map<String, String> parseAdminTotpSecrets(String rawConfiguration) {
        if (!StringUtils.hasText(rawConfiguration)) {
            return Map.of();
        }

        Map<String, String> parsed = new LinkedHashMap<>();
        for (String rawEntry : rawConfiguration.split(";", -1)) {
            String entry = rawEntry == null ? "" : rawEntry.trim();
            if (!StringUtils.hasText(entry)) {
                throw new IllegalStateException(
                        "app.security.admin-totp-secrets must not contain empty entries");
            }
            int separator = entry.indexOf('=');
            if (separator <= 0 || separator == entry.length() - 1) {
                throw new IllegalStateException(
                        "app.security.admin-totp-secrets entries must use username=Base32Secret format");
            }

            String username = normalize(entry.substring(0, separator));
            String secret = normalizeSecret(entry.substring(separator + 1));
            if (!SAFE_USERNAME.matcher(username).matches() || !StringUtils.hasText(secret)) {
                throw new IllegalStateException(
                        "app.security.admin-totp-secrets contains an invalid username or secret entry");
            }
            if (parsed.putIfAbsent(username, secret) != null) {
                throw new IllegalStateException(
                        "app.security.admin-totp-secrets contains duplicate usernames");
            }
        }
        return Collections.unmodifiableMap(parsed);
    }

    private boolean isProdProfileActive() {
        if (environment == null) {
            return false;
        }
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                return true;
            }
        }
        return false;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeSecret(String value) {
        return value == null ? "" : value.trim();
    }

    private static String canonicalSecret(String value) {
        return normalizeSecret(value)
                .replace(" ", "")
                .replace("-", "")
                .replace("=", "")
                .toUpperCase(Locale.ROOT);
    }
}
