package com.tibet.tourism.common.security.antibot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.tibet.tourism.common.security.antibot.infra.BehaviorLogRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.OptionalDouble;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

@ExtendWith(MockitoExtension.class)
class RiskAssessmentServiceTest {

    @Mock
    private RecaptchaService recaptchaService;

    @Mock
    private DeviceFingerprintService fingerprintService;

    @Mock
    private BehaviorAnalysisService behaviorService;

    @Mock
    private BehaviorLogRepository behaviorLogRepository;

    private AntibotProperties properties;
    private RiskAssessmentService service;
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;
    private boolean originalAdditive;
    private Level originalLevel;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(RiskAssessmentService.class);
        originalAdditive = logger.isAdditive();
        originalLevel = logger.getLevel();
        logger.setAdditive(false);
        logger.setLevel(Level.DEBUG);
        appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);

        properties = new AntibotProperties();
        properties.setEnabled(true);
        properties.getRecaptcha().setEnabled(true);
        properties.getRecaptcha().setSiteKey("test-site");
        properties.getRecaptcha().setSecretKey("test-secret");

        Executor directExecutor = Runnable::run;
        service = new RiskAssessmentService(
                recaptchaService,
                fingerprintService,
                behaviorService,
                behaviorLogRepository,
                properties,
                directExecutor);

        lenient().when(behaviorService.analyze(any())).thenReturn(
                new BehaviorAnalysisService.BehaviorMetrics(0, 0, 0, 0, 0, 0));
        lenient().when(fingerprintService.assess(any(), any())).thenReturn(0);
    }

    @AfterEach
    void detachAppender() {
        logger.detachAppender(appender);
        logger.setAdditive(originalAdditive);
        logger.setLevel(originalLevel);
        appender.stop();
    }

    @Test
    void missingRecaptchaResultForEnabledRecaptchaChallengesInsteadOfAllowing() {
        when(recaptchaService.verify(eq(null), eq("203.0.113.10"))).thenReturn(OptionalDouble.empty());

        RiskResult result = service.assess(
                null,
                null,
                42L,
                null,
                "203.0.113.10",
                "/api/bookings");

        assertThat(result.recaptchaRisk()).isEqualTo(100.0);
        assertThat(result.finalScore()).isGreaterThanOrEqualTo(properties.getRisk().getChallengeThreshold());
        assertThat(result.decision()).isEqualTo(RiskResult.Decision.CHALLENGE);
    }

    @Test
    void recaptchaScoreBelowConfiguredMinimumChallengesEvenWhenWeightedRiskIsLow() {
        when(recaptchaService.verify(eq("low-score-token"), eq("203.0.113.10")))
                .thenReturn(OptionalDouble.of(0.4));

        RiskResult result = service.assess(
                "low-score-token",
                null,
                42L,
                null,
                "203.0.113.10",
                "/api/hotel-bookings");

        assertThat(result.recaptchaRisk()).isEqualTo(60.0);
        assertThat(result.finalScore()).isGreaterThanOrEqualTo(properties.getRisk().getChallengeThreshold());
        assertThat(result.decision()).isEqualTo(RiskResult.Decision.CHALLENGE);
    }

    @Test
    void successfulV2RecaptchaScoreAllowsLowRiskRequest() {
        when(recaptchaService.verify(eq("v2-token"), eq("203.0.113.10")))
                .thenReturn(OptionalDouble.of(1.0));

        RiskResult result = service.assess(
                "v2-token",
                null,
                42L,
                null,
                "203.0.113.10",
                "/api/hotel-bookings");

        assertThat(result.recaptchaRisk()).isEqualTo(0.0);
        assertThat(result.finalScore()).isEqualTo(0.0);
        assertThat(result.decision()).isEqualTo(RiskResult.Decision.ALLOW);
    }

    @Test
    void disabledRecaptchaDoesNotForceChallenge() {
        properties.getRecaptcha().setEnabled(false);

        RiskResult result = service.assess(
                null,
                null,
                42L,
                null,
                "203.0.113.10",
                "/api/bookings");

        assertThat(result.decision()).isEqualTo(RiskResult.Decision.ALLOW);
        assertThat(result.finalScore()).isEqualTo(0.0);
    }

    @Test
    void incompleteRecaptchaConfigurationChallengesInsteadOfAllowing() {
        properties.getRecaptcha().setSiteKey("");
        properties.getRecaptcha().setSecretKey("");

        RiskResult result = service.assess(
                null,
                null,
                42L,
                null,
                "203.0.113.10",
                "/api/hotel-bookings");

        assertThat(result.recaptchaRisk()).isEqualTo(100.0);
        assertThat(result.finalScore()).isGreaterThanOrEqualTo(properties.getRisk().getChallengeThreshold());
        assertThat(result.decision()).isEqualTo(RiskResult.Decision.CHALLENGE);
        verify(recaptchaService).verify(eq(null), eq("203.0.113.10"));
    }

    @Test
    void partialAssessmentFailureLogsSanitizedSummaryWithoutRawPayload() {
        String rawFingerprint = "raw-fingerprint token=fingerprint-secret";
        when(recaptchaService.verify(eq("v2-token"), eq("203.0.113.10")))
                .thenReturn(OptionalDouble.of(1.0));
        when(behaviorService.analyze(any())).thenThrow(new RuntimeException(
                "behavior raw payload token=partial-secret"));
        when(fingerprintService.fingerprintLabel(eq(rawFingerprint))).thenReturn("fp#safe123");
        when(fingerprintService.userLabel(eq(42L))).thenReturn("user#safe123");

        RiskResult result = service.assess(
                "v2-token",
                rawFingerprint,
                42L,
                null,
                "203.0.113.10",
                "/api/orders?email=suli@example.com&token=query-secret");

        assertThat(result.decision()).isEqualTo(RiskResult.Decision.CHALLENGE);
        assertThat(formattedLogMessages())
                .anySatisfy(message -> assertThat(message)
                        .contains("Antibot assessment partially failed")
                        .contains("messageHash="));
        assertNoRiskSecretsInLogs();
    }

    @Test
    void behaviorDecodeAndPersistFailuresLogSanitizedSummaries() {
        String rawFingerprint = "raw-fingerprint token=fingerprint-secret";
        when(recaptchaService.verify(eq("v2-token"), eq("203.0.113.10")))
                .thenReturn(OptionalDouble.of(1.0));
        when(fingerprintService.fingerprintLabel(eq(rawFingerprint))).thenReturn("fp#safe123");
        when(fingerprintService.userLabel(eq(42L))).thenReturn("user#safe123");
        when(behaviorLogRepository.save(any())).thenThrow(new RuntimeException(
                "insert failed token=behavior-secret"));
        String behaviorPayload = "{\"token\":\"behavior-secret\"";
        String behaviorHeader = Base64.getEncoder()
                .encodeToString(behaviorPayload.getBytes(StandardCharsets.UTF_8));

        RiskResult result = service.assess(
                "v2-token",
                rawFingerprint,
                42L,
                behaviorHeader,
                "203.0.113.10",
                "/api/orders?email=suli@example.com&token=query-secret");

        assertThat(result.decision()).isEqualTo(RiskResult.Decision.ALLOW);
        assertThat(formattedLogMessages())
                .anySatisfy(message -> assertThat(message)
                        .contains("endpoint#")
                        .contains("user#safe123")
                        .contains("fp#safe123"));
        assertThat(formattedLogMessages())
                .anySatisfy(message -> assertThat(message)
                        .contains("Failed to decode behavior data")
                        .contains("messageHash="));
        assertThat(formattedLogMessages())
                .anySatisfy(message -> assertThat(message)
                        .contains("Failed to persist behavior log")
                        .contains("messageHash="));
        assertNoRiskSecretsInLogs();
        assertThat(formattedLogMessages()).allSatisfy(message -> assertThat(message)
                .doesNotContain("insert failed")
                .doesNotContain(behaviorPayload));
    }

    private void assertNoRiskSecretsInLogs() {
        assertThat(formattedLogMessages()).allSatisfy(message -> assertThat(message)
                .doesNotContain("raw-fingerprint")
                .doesNotContain("fingerprint-secret")
                .doesNotContain("partial-secret")
                .doesNotContain("behavior-secret")
                .doesNotContain("suli@example.com")
                .doesNotContain("query-secret"));
    }

    private java.util.List<String> formattedLogMessages() {
        return appender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }
}
