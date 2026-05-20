package com.tibet.tourism.common.security.antibot;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.security.antibot.infra.BehaviorLogRepository;
import java.util.OptionalDouble;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @BeforeEach
    void setUp() {
        properties = new AntibotProperties();
        properties.setEnabled(true);
        properties.getRecaptcha().setEnabled(true);
        properties.getRecaptcha().setSecretKey("test-secret");

        Executor directExecutor = Runnable::run;
        service = new RiskAssessmentService(
                recaptchaService,
                fingerprintService,
                behaviorService,
                behaviorLogRepository,
                properties,
                directExecutor);

        when(behaviorService.analyze(any())).thenReturn(
                new BehaviorAnalysisService.BehaviorMetrics(0, 0, 0, 0, 0, 0));
        when(fingerprintService.assess(any(), any())).thenReturn(0);
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
}
