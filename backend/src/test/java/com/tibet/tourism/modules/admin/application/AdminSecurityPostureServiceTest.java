package com.tibet.tourism.modules.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.security.CsrfTokenService;
import com.tibet.tourism.common.security.JwtUtils;
import com.tibet.tourism.common.security.TokenRevocationService;
import com.tibet.tourism.common.security.antibot.AntibotProperties;
import com.tibet.tourism.modules.auth.application.AdminMfaPolicy;
import com.tibet.tourism.modules.admin.web.dto.SecurityPostureResponse;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.core.env.Environment;
import org.springframework.test.util.ReflectionTestUtils;

class AdminSecurityPostureServiceTest {

    private HealthEndpoint healthEndpoint;
    private AdminSecurityPostureService service;
    private AdminMfaPolicy adminMfaPolicy;
    private UserRepository userRepository;

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
        adminMfaPolicy = mock(AdminMfaPolicy.class);
        userRepository = mock(UserRepository.class);
        when(userRepository.findUsernamesByRole(User.Role.ADMIN))
                .thenReturn(List.of("lzh", "content-admin"));
        when(adminMfaPolicy.hasConfiguredSecret("lzh")).thenReturn(true);
        when(adminMfaPolicy.hasConfiguredSecret("content-admin")).thenReturn(true);

        service = new AdminSecurityPostureService(
                environment,
                healthEndpoint,
                jwtUtils,
                csrfTokenService,
                tokenRevocationService,
                antibotProperties,
                adminMfaPolicy,
                userRepository);

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
                .contains("PUBLIC_DOCS_ENABLED", "JWT_CONFIGURED", "RATE_LIMIT_REDIS_FAIL_CLOSED",
                        "ADMIN_MFA_CONFIGURATION");
        assertThat(adminMfaFinding(response).getStatus()).isEqualTo("PASS");
    }

    @Test
    void reportsInfoWhenDatabaseContainsNoAdministrators() {
        when(userRepository.findUsernamesByRole(User.Role.ADMIN)).thenReturn(List.of());

        SecurityPostureResponse response = service.getSecurityPosture();

        SecurityPostureResponse.Finding finding = adminMfaFinding(response);
        assertThat(finding.getStatus()).isEqualTo("INFO");
        assertThat(finding.getMessage()).isEqualTo("No administrator accounts exist");
        assertThat(response.getScore()).isEqualTo(100);
    }

    @Test
    void missingAdministratorSecretFailsFindingAndReducesScoreWithoutNamingAccount() {
        when(userRepository.findUsernamesByRole(User.Role.ADMIN)).thenReturn(List.of(
                "lzh", "content-admin", "ops-admin"));
        when(adminMfaPolicy.hasConfiguredSecret("ops-admin")).thenReturn(false);

        SecurityPostureResponse response = service.getSecurityPosture();

        SecurityPostureResponse.Finding finding = adminMfaFinding(response);
        assertThat(finding.getStatus()).isEqualTo("FAIL");
        assertThat(finding.getSeverity()).isEqualTo("HIGH");
        assertThat(finding.getMessage())
                .isEqualTo("Administrator MFA is missing for 1 account(s)")
                .doesNotContain("ops-admin", "content-admin", "lzh");
        assertThat(response.getScore()).isEqualTo(80);
        assertThat(response.getStatus()).isEqualTo("DEGRADED");
    }

    @Test
    void databaseFailureFailsClosedWithoutLeakingQueryDetails() {
        when(userRepository.findUsernamesByRole(User.Role.ADMIN))
                .thenThrow(new IllegalStateException("jdbc:mysql://secret-host/users"));

        SecurityPostureResponse response = service.getSecurityPosture();

        SecurityPostureResponse.Finding finding = adminMfaFinding(response);
        assertThat(finding.getStatus()).isEqualTo("FAIL");
        assertThat(finding.getMessage())
                .isEqualTo("Administrator MFA coverage could not be verified")
                .doesNotContain("jdbc", "secret-host", "users");
        assertThat(response.getScore()).isEqualTo(80);
    }

    private SecurityPostureResponse.Finding adminMfaFinding(SecurityPostureResponse response) {
        return response.getFindings().stream()
                .filter(finding -> "ADMIN_MFA_CONFIGURATION".equals(finding.getId()))
                .findFirst()
                .orElseThrow();
    }

}
