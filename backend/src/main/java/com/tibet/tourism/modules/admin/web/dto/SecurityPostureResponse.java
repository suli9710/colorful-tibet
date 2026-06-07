package com.tibet.tourism.modules.admin.web.dto;

import java.time.LocalDateTime;
import java.util.List;

public class SecurityPostureResponse {

    private String status;
    private int score;
    private LocalDateTime generatedAt;
    private EnvironmentSnapshot environment;
    private ExposureSnapshot exposure;
    private AuthenticationSnapshot authentication;
    private ProtectionSnapshot protections;
    private DataProtectionSnapshot dataProtection;
    private DependencySnapshot dependencies;
    private List<Finding> findings;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public LocalDateTime getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(LocalDateTime generatedAt) {
        this.generatedAt = generatedAt;
    }

    public EnvironmentSnapshot getEnvironment() {
        return environment;
    }

    public void setEnvironment(EnvironmentSnapshot environment) {
        this.environment = environment;
    }

    public ExposureSnapshot getExposure() {
        return exposure;
    }

    public void setExposure(ExposureSnapshot exposure) {
        this.exposure = exposure;
    }

    public AuthenticationSnapshot getAuthentication() {
        return authentication;
    }

    public void setAuthentication(AuthenticationSnapshot authentication) {
        this.authentication = authentication;
    }

    public ProtectionSnapshot getProtections() {
        return protections;
    }

    public void setProtections(ProtectionSnapshot protections) {
        this.protections = protections;
    }

    public DataProtectionSnapshot getDataProtection() {
        return dataProtection;
    }

    public void setDataProtection(DataProtectionSnapshot dataProtection) {
        this.dataProtection = dataProtection;
    }

    public DependencySnapshot getDependencies() {
        return dependencies;
    }

    public void setDependencies(DependencySnapshot dependencies) {
        this.dependencies = dependencies;
    }

    public List<Finding> getFindings() {
        return findings;
    }

    public void setFindings(List<Finding> findings) {
        this.findings = findings;
    }

    public static class EnvironmentSnapshot {
        private List<String> activeProfiles;
        private boolean strictSecretsRequired;

        public List<String> getActiveProfiles() {
            return activeProfiles;
        }

        public void setActiveProfiles(List<String> activeProfiles) {
            this.activeProfiles = activeProfiles;
        }

        public boolean isStrictSecretsRequired() {
            return strictSecretsRequired;
        }

        public void setStrictSecretsRequired(boolean strictSecretsRequired) {
            this.strictSecretsRequired = strictSecretsRequired;
        }
    }

    public static class ExposureSnapshot {
        private boolean publicDocsEnabled;
        private boolean publicMetricsEnabled;
        private boolean actuatorHealthPublic;
        private boolean actuatorInfoAdminOnly;
        private boolean corsConfigured;
        private boolean trustedProxyHeadersEnabled;

        public boolean isPublicDocsEnabled() {
            return publicDocsEnabled;
        }

        public void setPublicDocsEnabled(boolean publicDocsEnabled) {
            this.publicDocsEnabled = publicDocsEnabled;
        }

        public boolean isPublicMetricsEnabled() {
            return publicMetricsEnabled;
        }

        public void setPublicMetricsEnabled(boolean publicMetricsEnabled) {
            this.publicMetricsEnabled = publicMetricsEnabled;
        }

        public boolean isActuatorHealthPublic() {
            return actuatorHealthPublic;
        }

        public void setActuatorHealthPublic(boolean actuatorHealthPublic) {
            this.actuatorHealthPublic = actuatorHealthPublic;
        }

        public boolean isActuatorInfoAdminOnly() {
            return actuatorInfoAdminOnly;
        }

        public void setActuatorInfoAdminOnly(boolean actuatorInfoAdminOnly) {
            this.actuatorInfoAdminOnly = actuatorInfoAdminOnly;
        }

        public boolean isCorsConfigured() {
            return corsConfigured;
        }

        public void setCorsConfigured(boolean corsConfigured) {
            this.corsConfigured = corsConfigured;
        }

        public boolean isTrustedProxyHeadersEnabled() {
            return trustedProxyHeadersEnabled;
        }

        public void setTrustedProxyHeadersEnabled(boolean trustedProxyHeadersEnabled) {
            this.trustedProxyHeadersEnabled = trustedProxyHeadersEnabled;
        }
    }

    public static class AuthenticationSnapshot {
        private boolean jwtConfigured;
        private boolean jwtIssuerConfigured;
        private boolean jwtAudienceConfigured;
        private int jwtExpirationMs;
        private boolean cookieSecure;
        private String cookieSameSite;
        private boolean csrfConfigured;
        private boolean superAdminTotpConfigured;
        private boolean tokenRevocationRedisEnabled;

        public boolean isJwtConfigured() {
            return jwtConfigured;
        }

        public void setJwtConfigured(boolean jwtConfigured) {
            this.jwtConfigured = jwtConfigured;
        }

        public boolean isJwtIssuerConfigured() {
            return jwtIssuerConfigured;
        }

        public void setJwtIssuerConfigured(boolean jwtIssuerConfigured) {
            this.jwtIssuerConfigured = jwtIssuerConfigured;
        }

        public boolean isJwtAudienceConfigured() {
            return jwtAudienceConfigured;
        }

        public void setJwtAudienceConfigured(boolean jwtAudienceConfigured) {
            this.jwtAudienceConfigured = jwtAudienceConfigured;
        }

        public int getJwtExpirationMs() {
            return jwtExpirationMs;
        }

        public void setJwtExpirationMs(int jwtExpirationMs) {
            this.jwtExpirationMs = jwtExpirationMs;
        }

        public boolean isCookieSecure() {
            return cookieSecure;
        }

        public void setCookieSecure(boolean cookieSecure) {
            this.cookieSecure = cookieSecure;
        }

        public String getCookieSameSite() {
            return cookieSameSite;
        }

        public void setCookieSameSite(String cookieSameSite) {
            this.cookieSameSite = cookieSameSite;
        }

        public boolean isCsrfConfigured() {
            return csrfConfigured;
        }

        public void setCsrfConfigured(boolean csrfConfigured) {
            this.csrfConfigured = csrfConfigured;
        }

        public boolean isSuperAdminTotpConfigured() {
            return superAdminTotpConfigured;
        }

        public void setSuperAdminTotpConfigured(boolean superAdminTotpConfigured) {
            this.superAdminTotpConfigured = superAdminTotpConfigured;
        }

        public boolean isTokenRevocationRedisEnabled() {
            return tokenRevocationRedisEnabled;
        }

        public void setTokenRevocationRedisEnabled(boolean tokenRevocationRedisEnabled) {
            this.tokenRevocationRedisEnabled = tokenRevocationRedisEnabled;
        }
    }

    public static class ProtectionSnapshot {
        private boolean rateLimitEnabled;
        private boolean rateLimitRedisEnabled;
        private boolean bruteForceEnabled;
        private boolean bruteForceRedisEnabled;
        private boolean antibotEnabled;
        private boolean recaptchaConfigured;

        public boolean isRateLimitEnabled() {
            return rateLimitEnabled;
        }

        public void setRateLimitEnabled(boolean rateLimitEnabled) {
            this.rateLimitEnabled = rateLimitEnabled;
        }

        public boolean isRateLimitRedisEnabled() {
            return rateLimitRedisEnabled;
        }

        public void setRateLimitRedisEnabled(boolean rateLimitRedisEnabled) {
            this.rateLimitRedisEnabled = rateLimitRedisEnabled;
        }

        public boolean isBruteForceEnabled() {
            return bruteForceEnabled;
        }

        public void setBruteForceEnabled(boolean bruteForceEnabled) {
            this.bruteForceEnabled = bruteForceEnabled;
        }

        public boolean isBruteForceRedisEnabled() {
            return bruteForceRedisEnabled;
        }

        public void setBruteForceRedisEnabled(boolean bruteForceRedisEnabled) {
            this.bruteForceRedisEnabled = bruteForceRedisEnabled;
        }

        public boolean isAntibotEnabled() {
            return antibotEnabled;
        }

        public void setAntibotEnabled(boolean antibotEnabled) {
            this.antibotEnabled = antibotEnabled;
        }

        public boolean isRecaptchaConfigured() {
            return recaptchaConfigured;
        }

        public void setRecaptchaConfigured(boolean recaptchaConfigured) {
            this.recaptchaConfigured = recaptchaConfigured;
        }
    }

    public static class DataProtectionSnapshot {
        private boolean piiKeysConfigured;
        private boolean piiActiveKeyConfigured;
        private boolean legacyPiiKeyConfigured;
        private boolean piiMigrationEnabled;

        public boolean isPiiKeysConfigured() {
            return piiKeysConfigured;
        }

        public void setPiiKeysConfigured(boolean piiKeysConfigured) {
            this.piiKeysConfigured = piiKeysConfigured;
        }

        public boolean isPiiActiveKeyConfigured() {
            return piiActiveKeyConfigured;
        }

        public void setPiiActiveKeyConfigured(boolean piiActiveKeyConfigured) {
            this.piiActiveKeyConfigured = piiActiveKeyConfigured;
        }

        public boolean isLegacyPiiKeyConfigured() {
            return legacyPiiKeyConfigured;
        }

        public void setLegacyPiiKeyConfigured(boolean legacyPiiKeyConfigured) {
            this.legacyPiiKeyConfigured = legacyPiiKeyConfigured;
        }

        public boolean isPiiMigrationEnabled() {
            return piiMigrationEnabled;
        }

        public void setPiiMigrationEnabled(boolean piiMigrationEnabled) {
            this.piiMigrationEnabled = piiMigrationEnabled;
        }
    }

    public static class DependencySnapshot {
        private String database;
        private String redis;
        private String scrapling;
        private boolean aiProviderConfigured;
        private boolean paymentCallbackSecretConfigured;

        public String getDatabase() {
            return database;
        }

        public void setDatabase(String database) {
            this.database = database;
        }

        public String getRedis() {
            return redis;
        }

        public void setRedis(String redis) {
            this.redis = redis;
        }

        public String getScrapling() {
            return scrapling;
        }

        public void setScrapling(String scrapling) {
            this.scrapling = scrapling;
        }

        public boolean isAiProviderConfigured() {
            return aiProviderConfigured;
        }

        public void setAiProviderConfigured(boolean aiProviderConfigured) {
            this.aiProviderConfigured = aiProviderConfigured;
        }

        public boolean isPaymentCallbackSecretConfigured() {
            return paymentCallbackSecretConfigured;
        }

        public void setPaymentCallbackSecretConfigured(boolean paymentCallbackSecretConfigured) {
            this.paymentCallbackSecretConfigured = paymentCallbackSecretConfigured;
        }
    }

    public static class Finding {
        private String id;
        private String severity;
        private String status;
        private String message;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getSeverity() {
            return severity;
        }

        public void setSeverity(String severity) {
            this.severity = severity;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
