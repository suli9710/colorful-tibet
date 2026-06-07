package com.tibet.tourism.common.security;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ProductionSafetyValidator {

    private final Environment environment;
    private final boolean cookieSecure;
    private final boolean requireStrongSecrets;
    private final boolean mockCallbackEnabled;
    private final String scraplingApiKey;

    public ProductionSafetyValidator(
            Environment environment,
            @Value("${app.security.cookie-secure:true}") boolean cookieSecure,
            @Value("${app.security.require-strong-secrets:false}") boolean requireStrongSecrets,
            @Value("${app.payments.mock-callback-enabled:false}") boolean mockCallbackEnabled,
            @Value("${scrapling.service.api-key:}") String scraplingApiKey) {
        this.environment = environment;
        this.cookieSecure = cookieSecure;
        this.requireStrongSecrets = requireStrongSecrets;
        this.mockCallbackEnabled = mockCallbackEnabled;
        this.scraplingApiKey = scraplingApiKey;
    }

    @PostConstruct
    public void validateProductionSafety() {
        validate(
                isProdProfileActive(environment),
                cookieSecure,
                requireStrongSecrets,
                mockCallbackEnabled,
                scraplingApiKey);
    }

    static void validate(
            boolean prodProfile,
            boolean cookieSecure,
            boolean requireStrongSecrets,
            boolean mockCallbackEnabled,
            String scraplingApiKey) {
        if (prodProfile) {
            if (!cookieSecure) {
                throw new IllegalStateException("Production profile requires app.security.cookie-secure=true");
            }
            if (!requireStrongSecrets) {
                throw new IllegalStateException("Production profile requires app.security.require-strong-secrets=true");
            }
            if (mockCallbackEnabled) {
                throw new IllegalStateException("Production profile requires app.payments.mock-callback-enabled=false");
            }
            if (!StringUtils.hasText(scraplingApiKey)) {
                throw new IllegalStateException("Production profile requires scrapling.service.api-key");
            }
        }
    }

    private static boolean isProdProfileActive(Environment environment) {
        return environment != null
                && Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
    }
}
