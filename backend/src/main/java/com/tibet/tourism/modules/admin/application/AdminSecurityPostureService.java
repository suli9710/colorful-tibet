package com.tibet.tourism.modules.admin.application;

import com.tibet.tourism.common.security.CsrfTokenService;
import com.tibet.tourism.common.security.JwtUtils;
import com.tibet.tourism.common.security.TokenRevocationService;
import com.tibet.tourism.common.security.antibot.AntibotProperties;
import com.tibet.tourism.modules.admin.web.dto.SecurityPostureResponse;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.HealthComponent;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.boot.actuate.health.Status;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminSecurityPostureService {

    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");

    private final Environment environment;
    private final HealthEndpoint healthEndpoint;
    private final JwtUtils jwtUtils;
    private final CsrfTokenService csrfTokenService;
    private final TokenRevocationService tokenRevocationService;
    private final AntibotProperties antibotProperties;

    @Value("${app.security.public-docs-enabled:false}")
    private boolean publicDocsEnabled;

    @Value("${app.security.public-metrics-enabled:false}")
    private boolean publicMetricsEnabled;

    @Value("${app.security.trust-proxy-headers:false}")
    private boolean trustProxyHeaders;

    @Value("${app.security.cookie-secure:true}")
    private boolean cookieSecure;

    @Value("${app.security.pii-keys:}")
    private String piiKeys;

    @Value("${app.security.pii-active-kid:}")
    private String piiActiveKid;

    @Value("${app.security.pii-encryption-key:}")
    private String legacyPiiKey;

    @Value("${app.security.pii-migration-enabled:false}")
    private boolean piiMigrationEnabled;

    @Value("${app.security.super-admin-totp-secret:}")
    private String superAdminTotpSecret;

    @Value("${app.security.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    @Value("${app.security.rate-limit.redis-enabled:true}")
    private boolean rateLimitRedisEnabled;

    @Value("${app.security.brute-force.enabled:true}")
    private boolean bruteForceEnabled;

    @Value("${app.security.brute-force.redis-enabled:true}")
    private boolean bruteForceRedisEnabled;

    @Value("${app.payments.mock-callback-secret:}")
    private String paymentCallbackSecret;

    @Value("${jwt.secret:}")
    private String jwtSecret;

    @Value("${jwt.issuer:}")
    private String jwtIssuer;

    @Value("${jwt.audience:}")
    private String jwtAudience;

    @Value("${jwt.expiration:0}")
    private int jwtExpirationMs;

    @Value("${app.security.require-strong-secrets:false}")
    private boolean requireStrongSecrets;

    @Value("${scrapling.service.url:}")
    private String scraplingServiceUrl;

    @Value("${ark.api.key:}")
    private String arkApiKey;

    @Value("${doubao.api.key:}")
    private String doubaoApiKey;

    public AdminSecurityPostureService(Environment environment,
                                       HealthEndpoint healthEndpoint,
                                       JwtUtils jwtUtils,
                                       CsrfTokenService csrfTokenService,
                                       TokenRevocationService tokenRevocationService,
                                       AntibotProperties antibotProperties) {
        this.environment = environment;
        this.healthEndpoint = healthEndpoint;
        this.jwtUtils = jwtUtils;
        this.csrfTokenService = csrfTokenService;
        this.tokenRevocationService = tokenRevocationService;
        this.antibotProperties = antibotProperties;
    }

    public SecurityPostureResponse getSecurityPosture() {
        SecurityPostureResponse response = new SecurityPostureResponse();
        response.setGeneratedAt(LocalDateTime.now(BEIJING_ZONE));

        boolean prodProfile = isProdProfileActive();
        boolean strictSecrets = requireStrongSecrets || prodProfile;

        SecurityPostureResponse.EnvironmentSnapshot environmentSnapshot = new SecurityPostureResponse.EnvironmentSnapshot();
        environmentSnapshot.setActiveProfiles(activeProfiles());
        environmentSnapshot.setStrictSecretsRequired(strictSecrets);
        response.setEnvironment(environmentSnapshot);

        SecurityPostureResponse.ExposureSnapshot exposureSnapshot = new SecurityPostureResponse.ExposureSnapshot();
        exposureSnapshot.setPublicDocsEnabled(publicDocsEnabled);
        exposureSnapshot.setPublicMetricsEnabled(publicMetricsEnabled);
        exposureSnapshot.setActuatorHealthPublic(true);
        exposureSnapshot.setActuatorInfoAdminOnly(true);
        exposureSnapshot.setCorsConfigured(hasText(listProperty("app.cors.allowed-origins", "CORS_ALLOWED_ORIGINS")));
        exposureSnapshot.setTrustedProxyHeadersEnabled(trustProxyHeaders);
        response.setExposure(exposureSnapshot);

        SecurityPostureResponse.AuthenticationSnapshot authenticationSnapshot = new SecurityPostureResponse.AuthenticationSnapshot();
        authenticationSnapshot.setJwtConfigured(hasText(jwtSecret));
        authenticationSnapshot.setJwtIssuerConfigured(hasText(jwtIssuer));
        authenticationSnapshot.setJwtAudienceConfigured(hasText(jwtAudience));
        authenticationSnapshot.setJwtExpirationMs(jwtExpirationMs);
        authenticationSnapshot.setCookieSecure(cookieSecure);
        authenticationSnapshot.setCookieSameSite("Strict");
        authenticationSnapshot.setCsrfConfigured(isCsrfConfigured());
        authenticationSnapshot.setSuperAdminTotpConfigured(hasText(superAdminTotpSecret));
        authenticationSnapshot.setTokenRevocationRedisEnabled(tokenRevocationService != null && readBool("app.security.jwt-revocation.redis-enabled", true));
        response.setAuthentication(authenticationSnapshot);

        SecurityPostureResponse.ProtectionSnapshot protectionSnapshot = new SecurityPostureResponse.ProtectionSnapshot();
        protectionSnapshot.setRateLimitEnabled(rateLimitEnabled);
        protectionSnapshot.setRateLimitRedisEnabled(rateLimitRedisEnabled);
        protectionSnapshot.setBruteForceEnabled(bruteForceEnabled);
        protectionSnapshot.setBruteForceRedisEnabled(bruteForceRedisEnabled);
        protectionSnapshot.setAntibotEnabled(antibotProperties != null && antibotProperties.isEnabled());
        protectionSnapshot.setRecaptchaConfigured(isRecaptchaConfigured());
        response.setProtections(protectionSnapshot);

        SecurityPostureResponse.DataProtectionSnapshot dataProtectionSnapshot = new SecurityPostureResponse.DataProtectionSnapshot();
        dataProtectionSnapshot.setPiiKeysConfigured(hasText(piiKeys));
        dataProtectionSnapshot.setPiiActiveKeyConfigured(hasText(piiActiveKid));
        dataProtectionSnapshot.setLegacyPiiKeyConfigured(hasText(legacyPiiKey));
        dataProtectionSnapshot.setPiiMigrationEnabled(piiMigrationEnabled);
        response.setDataProtection(dataProtectionSnapshot);

        SecurityPostureResponse.DependencySnapshot dependencySnapshot = new SecurityPostureResponse.DependencySnapshot();
        dependencySnapshot.setDatabase(resolveHealthComponent("db"));
        dependencySnapshot.setRedis(resolveHealthComponent("redis"));
        dependencySnapshot.setScrapling(hasText(scraplingServiceUrl)
                ? resolveHealthComponent("scrapling")
                : "DISABLED");
        dependencySnapshot.setAiProviderConfigured(hasText(arkApiKey) || hasText(doubaoApiKey));
        dependencySnapshot.setPaymentCallbackSecretConfigured(hasText(paymentCallbackSecret));
        response.setDependencies(dependencySnapshot);

        List<SecurityPostureResponse.Finding> findings = new ArrayList<>();
        addFinding(findings, "PUBLIC_DOCS_ENABLED", publicDocsEnabled ? "WARN" : "PASS",
                publicDocsEnabled ? "Open API docs are enabled" : "Open API docs are restricted");
        addFinding(findings, "PUBLIC_METRICS_ENABLED", publicMetricsEnabled ? "WARN" : "PASS",
                publicMetricsEnabled ? "Prometheus is public" : "Prometheus is restricted");
        addFinding(findings, "JWT_CONFIGURED", hasText(jwtSecret) ? "PASS" : "FAIL",
                hasText(jwtSecret) ? "JWT secret configured" : "JWT secret missing");
        addFinding(findings, "CSRF_CONFIGURED", isCsrfConfigured() ? "PASS" : "FAIL",
                isCsrfConfigured() ? "CSRF signing configured" : "CSRF signing secret missing");
        addFinding(findings, "RATE_LIMITING", rateLimitEnabled ? "PASS" : "WARN",
                rateLimitEnabled ? "Rate limiting enabled" : "Rate limiting disabled");
        addFinding(findings, "PII_KEYS", hasText(piiKeys) && hasText(piiActiveKid) ? "PASS" : "WARN",
                hasText(piiKeys) && hasText(piiActiveKid) ? "PII encryption configured" : "PII encryption incomplete");
        addFinding(findings, "PAYMENT_CALLBACK", hasText(paymentCallbackSecret) ? "PASS" : "WARN",
                hasText(paymentCallbackSecret) ? "Payment callback secret configured" : "Payment callback secret missing");
        addFinding(findings, "SCRAPLING_HEALTH", hasText(scraplingServiceUrl) ? "PASS" : "INFO",
                hasText(scraplingServiceUrl) ? "Scrapling endpoint configured" : "Scrapling disabled");
        response.setFindings(findings);

        response.setScore(score(response));
        response.setStatus(resolveStatus(response.getScore()));
        return response;
    }

    private boolean isCsrfConfigured() {
        return csrfTokenService != null && hasText(readText("app.security.csrf-signing-secret", ""));
    }

    private boolean isRecaptchaConfigured() {
        if (antibotProperties == null || !antibotProperties.isEnabled()) {
            return false;
        }
        AntibotProperties.Recaptcha recaptcha = antibotProperties.getRecaptcha();
        return recaptcha != null
                && recaptcha.isEnabled()
                && hasText(recaptcha.getSiteKey())
                && hasText(recaptcha.getSecretKey());
    }

    private boolean isProdProfileActive() {
        return environment != null
                && Arrays.stream(environment.getActiveProfiles()).anyMatch("prod"::equalsIgnoreCase);
    }

    private List<String> activeProfiles() {
        if (environment == null) {
            return List.of();
        }
        return Arrays.stream(environment.getActiveProfiles()).toList();
    }

    private boolean hasText(String value) {
        return StringUtils.hasText(value);
    }

    private boolean readBool(String key, boolean defaultValue) {
        String value = readText(key, null);
        if (value == null) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value);
    }

    private String listProperty(String key, String envKey) {
        String value = readText(key, null);
        if (hasText(value)) {
            return value;
        }
        String env = System.getenv(envKey);
        return env == null ? "" : env;
    }

    private String readText(String key, String defaultValue) {
        if (environment == null) {
            return defaultValue;
        }
        String value = environment.getProperty(key);
        return value == null ? defaultValue : value.trim();
    }

    private String resolveHealthComponent(String name) {
        try {
            HealthComponent component = healthEndpoint.healthForPath(name);
            if (component == null) {
                return "UNKNOWN";
            }
            Status status = component.getStatus();
            if (status == null) {
                return "UNKNOWN";
            }
            return switch (status.getCode()) {
                case "UP" -> "UP";
                case "DOWN" -> "DOWN";
                case "OUT_OF_SERVICE" -> "OUT_OF_SERVICE";
                default -> "UNKNOWN";
            };
        } catch (Exception exception) {
            return "UNKNOWN";
        }
    }

    private String resolveStatus(int score) {
        if (score >= 85) {
            return "READY";
        }
        if (score >= 65) {
            return "DEGRADED";
        }
        return "BLOCKED";
    }

    private void addFinding(List<SecurityPostureResponse.Finding> findings,
                            String id,
                            String status,
                            String message) {
        SecurityPostureResponse.Finding finding = new SecurityPostureResponse.Finding();
        finding.setId(id);
        finding.setSeverity("PASS".equals(status) ? "LOW" : "WARN".equals(status) ? "MEDIUM" : "HIGH");
        finding.setStatus(status);
        finding.setMessage(message);
        findings.add(finding);
    }

    private int score(SecurityPostureResponse response) {
        int score = 100;
        score -= response.getExposure().isPublicDocsEnabled() ? 8 : 0;
        score -= response.getExposure().isPublicMetricsEnabled() ? 10 : 0;
        score -= penalty(response.getAuthentication().isJwtConfigured(), 25, 0);
        score -= penalty(response.getAuthentication().isCsrfConfigured(), 15, 0);
        score -= penalty(response.getAuthentication().isSuperAdminTotpConfigured(), 10, 0);
        score -= penalty(response.getProtections().isRateLimitEnabled(), 12, 0);
        score -= penalty(response.getProtections().isBruteForceEnabled(), 10, 0);
        score -= penalty(response.getDataProtection().isPiiKeysConfigured(), 10, 0);
        score -= penalty(response.getDependencies().isPaymentCallbackSecretConfigured(), 5, 0);
        score -= dependencyPenalty(response.getDependencies().getDatabase());
        score -= dependencyPenalty(response.getDependencies().getRedis());
        score -= dependencyPenalty(response.getDependencies().getScrapling());
        return Math.max(0, score);
    }

    private int penalty(boolean pass, int failPenalty, int warningPenalty) {
        return pass ? warningPenalty : failPenalty;
    }

    private int dependencyPenalty(String status) {
        if ("UP".equals(status) || "DISABLED".equals(status)) {
            return 0;
        }
        if ("UNKNOWN".equals(status)) {
            return 8;
        }
        if ("OUT_OF_SERVICE".equals(status)) {
            return 20;
        }
        return 15;
    }
}
