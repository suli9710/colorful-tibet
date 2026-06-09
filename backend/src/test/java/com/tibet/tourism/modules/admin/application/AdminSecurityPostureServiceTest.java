package com.tibet.tourism.modules.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.security.CsrfTokenService;
import com.tibet.tourism.common.security.JwtUtils;
import com.tibet.tourism.common.security.TokenRevocationService;
import com.tibet.tourism.common.security.antibot.AntibotProperties;
import com.tibet.tourism.modules.admin.web.dto.SecurityPostureResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

class AdminSecurityPostureServiceTest {

    private HealthEndpoint healthEndpoint;
    private AdminSecurityPostureService service;

    @BeforeEach
    void setUp() {
        healthEndpoint = mock(HealthEndpoint.class);
        when(healthEndpoint.healthForPath("scrapling")).thenReturn(Health.up().build());
        when(healthEndpoint.healthForPath("db")).thenReturn(Health.up().build());
        when(healthEndpoint.healthForPath("redis")).thenReturn(Health.up().build());

        Environment environment = mock(Environment.class);
        when(environment.getActiveProfiles()).thenReturn(new String[] {"prod"});
        when(environment.getProperty("app.security.csrf-signing-secret")).thenReturn(
                "test-csrf-secret-that-is-long-enough-for-hmac-signing-2026-abcdef-abcdef");
        when(environment.getProperty("app.cors.allowed-origins")).thenReturn("https://example.com");
        when(environment.getProperty("app.security.jwt-revocation.redis-enabled")).thenReturn("true");

        JwtUtils jwtUtils = mock(JwtUtils.class);
        CsrfTokenService csrfTokenService = mock(CsrfTokenService.class);
        TokenRevocationService tokenRevocationService = mock(TokenRevocationService.class);
        AntibotProperties antibotProperties = new AntibotProperties();
        antibotProperties.setEnabled(true);
        AntibotProperties.Recaptcha recaptcha = new AntibotProperties.Recaptcha();
        recaptcha.setEnabled(true);
        recaptcha.setSiteKey("site");
        recaptcha.setSecretKey("secret");
        antibotProperties.setRecaptcha(recaptcha);

        service = new AdminSecurityPostureService(
                environment,
                healthEndpoint,
                jwtUtils,
                csrfTokenService,
                tokenRevocationService,
                antibotProperties);

        ReflectionTestUtils.setField(service, "publicDocsEnabled", false);
        ReflectionTestUtils.setField(service, "publicMetricsEnabled", false);
        ReflectionTestUtils.setField(service, "trustProxyHeaders", false);
        ReflectionTestUtils.setField(service, "cookieSecure", true);
        ReflectionTestUtils.setField(service, "piiKeys", "kid-1:abc");
        ReflectionTestUtils.setField(service, "piiActiveKid", "kid-1");
        ReflectionTestUtils.setField(service, "legacyPiiKey", "");
        ReflectionTestUtils.setField(service, "piiMigrationEnabled", false);
        ReflectionTestUtils.setField(service, "superAdminTotpSecret", "totp-secret");
        ReflectionTestUtils.setField(service, "rateLimitEnabled", true);
        ReflectionTestUtils.setField(service, "rateLimitRedisEnabled", true);
        ReflectionTestUtils.setField(service, "rateLimitRedisFailClosed", true);
        ReflectionTestUtils.setField(service, "bruteForceEnabled", true);
        ReflectionTestUtils.setField(service, "bruteForceRedisEnabled", true);
        ReflectionTestUtils.setField(service, "bruteForceRedisFailClosed", true);
        ReflectionTestUtils.setField(service, "paymentCallbackSecret", "payment-secret");
        ReflectionTestUtils.setField(service, "jwtSecret", "jwt-secret-value-that-is-long-enough-for-tests-1234567890abcdef");
        ReflectionTestUtils.setField(service, "jwtIssuer", "colorful-tibet");
        ReflectionTestUtils.setField(service, "jwtAudience", "colorful-tibet-web");
        ReflectionTestUtils.setField(service, "jwtExpirationMs", 1800000);
        ReflectionTestUtils.setField(service, "scraplingServiceUrl", "http://scrapling:8000");
        ReflectionTestUtils.setField(service, "arkApiKey", "ark-key");
        ReflectionTestUtils.setField(service, "doubaoApiKey", "");
    }

    @Test
    void buildsSecurityPostureSnapshot() {
        SecurityPostureResponse response = service.getSecurityPosture();

        assertThat(response.getStatus()).isEqualTo("READY");
        assertThat(response.getScore()).isGreaterThan(0);
        assertThat(response.getGeneratedAt()).isNotNull();
        assertThat(response.getEnvironment().getActiveProfiles()).contains("prod");
        assertThat(response.getExposure().isActuatorHealthPublic()).isTrue();
        assertThat(response.getAuthentication().isJwtConfigured()).isTrue();
        assertThat(response.getProtections().isRateLimitEnabled()).isTrue();
        assertThat(response.getProtections().isRateLimitRedisFailClosed()).isTrue();
        assertThat(response.getProtections().isBruteForceRedisFailClosed()).isTrue();
        assertThat(response.getDataProtection().isPiiKeysConfigured()).isTrue();
        assertThat(response.getDependencies().getScrapling()).isEqualTo("UP");
        assertThat(response.getFindings()).extracting(SecurityPostureResponse.Finding::getId)
                .contains("PUBLIC_DOCS_ENABLED", "JWT_CONFIGURED", "RATE_LIMIT_REDIS_FAIL_CLOSED");
    }
}
