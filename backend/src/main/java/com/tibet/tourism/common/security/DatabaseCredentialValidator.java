package com.tibet.tourism.common.security;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DatabaseCredentialValidator {

    private final String username;
    private final String password;
    private final boolean requireStrongSecrets;
    private final Environment environment;

    public DatabaseCredentialValidator(
            @Value("${spring.datasource.username:}") String username,
            @Value("${spring.datasource.password:}") String password,
            @Value("${app.security.require-strong-secrets:false}") boolean requireStrongSecrets,
            Environment environment) {
        this.username = username;
        this.password = password;
        this.requireStrongSecrets = requireStrongSecrets;
        this.environment = environment;
    }

    @PostConstruct
    public void validateDatasourceConfiguration() {
        validate(username, password, requireStrongSecrets, isProdProfileActive(environment));
    }

    static void validate(String username, String password, boolean requireStrongSecrets, boolean prodProfile) {
        if (!requireStrongSecrets && !prodProfile) {
            return;
        }

        String normalizedUsername = username == null ? "" : username.trim();
        if (!StringUtils.hasText(normalizedUsername)) {
            throw new IllegalStateException("Datasource username must be configured in strict mode");
        }
        if ("root".equalsIgnoreCase(normalizedUsername)) {
            throw new IllegalStateException("Datasource username must not be root in strict mode");
        }

        String normalizedPassword = password == null ? "" : password.trim();
        if (!StringUtils.hasText(normalizedPassword) || looksUnresolvedPlaceholder(normalizedPassword)) {
            throw new IllegalStateException("Datasource password must be configured in strict mode");
        }
        if (isKnownUnsafePassword(normalizedPassword)) {
            throw new IllegalStateException("Datasource password must not use a placeholder or published default");
        }
    }

    private static boolean isProdProfileActive(Environment environment) {
        return environment != null
                && Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
    }

    private static boolean looksUnresolvedPlaceholder(String value) {
        return value.startsWith("${") && value.endsWith("}");
    }

    private static boolean isKnownUnsafePassword(String password) {
        String normalized = password.toLowerCase(Locale.ROOT);
        return normalized.equals("031224")
                || normalized.equals("root123456")
                || normalized.contains("change-me")
                || normalized.contains("replace-with")
                || normalized.contains("placeholder");
    }
}
