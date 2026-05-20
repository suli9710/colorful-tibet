package com.tibet.tourism.common.security.antibot;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tibet.tourism.common.security.antibot.domain.BehaviorLog;
import com.tibet.tourism.common.security.antibot.infra.BehaviorLogRepository;
import com.tibet.tourism.modules.user.domain.User;
import java.util.Base64;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.OptionalDouble;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class RiskAssessmentService {

    private static final Logger log = LoggerFactory.getLogger(RiskAssessmentService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final RecaptchaService recaptchaService;
    private final DeviceFingerprintService fingerprintService;
    private final BehaviorAnalysisService behaviorService;
    private final BehaviorLogRepository behaviorLogRepository;
    private final AntibotProperties properties;
    private final Executor executor;

    public RiskAssessmentService(RecaptchaService recaptchaService,
                                 DeviceFingerprintService fingerprintService,
                                 BehaviorAnalysisService behaviorService,
                                 BehaviorLogRepository behaviorLogRepository,
                                 AntibotProperties properties,
                                 @Qualifier("taskExecutor") Executor executor) {
        this.recaptchaService = recaptchaService;
        this.fingerprintService = fingerprintService;
        this.behaviorService = behaviorService;
        this.behaviorLogRepository = behaviorLogRepository;
        this.properties = properties;
        this.executor = executor;
    }

    public RiskResult assess(String recaptchaToken, String fingerprint,
                             Long userId, String behaviorDataHeader,
                             String remoteIp, String endpoint) {
        if (!properties.isEnabled()) {
            return RiskResult.allow();
        }

        BehaviorData behaviorData = decodeBehaviorData(behaviorDataHeader);

        CompletableFuture<OptionalDouble> recaptchaFuture = CompletableFuture.supplyAsync(
                () -> recaptchaService.verify(recaptchaToken, remoteIp), executor);

        CompletableFuture<BehaviorAnalysisService.BehaviorMetrics> behaviorFuture = CompletableFuture.supplyAsync(
                () -> behaviorService.analyze(behaviorData), executor);

        int fingerprintRisk = fingerprintService.assess(fingerprint, userId);

        OptionalDouble recaptchaScore;
        BehaviorAnalysisService.BehaviorMetrics metrics;
        try {
            OptionalDouble score = recaptchaFuture.join();
            recaptchaScore = score == null ? OptionalDouble.empty() : score;
            metrics = behaviorFuture.join();
        } catch (Exception e) {
            log.warn("Antibot assessment partially failed: {}", e.getMessage());
            recaptchaScore = OptionalDouble.empty();
            metrics = new BehaviorAnalysisService.BehaviorMetrics(0, 0, 0, 0, 0, 0);
        }

        AntibotProperties.Risk riskCfg = properties.getRisk();
        AntibotProperties.Recaptcha recaptchaCfg = properties.getRecaptcha();
        boolean recaptchaRequired = recaptchaCfg.isEnabled();

        double recaptchaRisk = recaptchaScore.isPresent()
                ? (1.0 - recaptchaScore.getAsDouble()) * 100.0
                : recaptchaRequired ? 100.0 : 0;

        double weightedScore;
        if (recaptchaScore.isPresent()) {
            weightedScore = recaptchaRisk * riskCfg.getRecaptchaWeight()
                    + metrics.risk() * riskCfg.getBehaviorWeight()
                    + fingerprintRisk * riskCfg.getFingerprintWeight();
        } else {
            double totalWeight = riskCfg.getBehaviorWeight() + riskCfg.getFingerprintWeight();
            weightedScore = totalWeight > 0
                    ? (metrics.risk() * riskCfg.getBehaviorWeight()
                       + fingerprintRisk * riskCfg.getFingerprintWeight()) / totalWeight * 100.0 / 100.0
                    : 0;
        }

        boolean recaptchaUnavailable = recaptchaRequired && recaptchaScore.isEmpty();
        boolean recaptchaBelowMinimum = recaptchaRequired
                && recaptchaScore.isPresent()
                && recaptchaScore.getAsDouble() < recaptchaCfg.getMinScore();
        if (recaptchaUnavailable || recaptchaBelowMinimum) {
            weightedScore = Math.max(weightedScore, riskCfg.getChallengeThreshold());
        }

        RiskResult.Decision decision;
        if (weightedScore >= riskCfg.getBlockThreshold()) {
            decision = RiskResult.Decision.BLOCK;
        } else if (weightedScore >= riskCfg.getChallengeThreshold()) {
            decision = RiskResult.Decision.CHALLENGE;
        } else {
            decision = RiskResult.Decision.ALLOW;
        }
        final double finalScore = weightedScore;

        RiskResult result = new RiskResult(recaptchaRisk, metrics.risk(), fingerprintRisk,
                finalScore, decision);

        log.info("Antibot assessment: endpoint={} user={} fp={} score={} decision={}",
                endpoint, userId, fingerprint != null ? fingerprint.substring(0, Math.min(8, fingerprint.length())) : "null",
                String.format("%.1f", finalScore), decision);

        final OptionalDouble captchaScore = recaptchaScore;
        final BehaviorAnalysisService.BehaviorMetrics m = metrics;
        CompletableFuture.runAsync(() -> persistLog(
                userId, endpoint, fingerprint, m, captchaScore, finalScore, decision), executor);

        return result;
    }

    private BehaviorData decodeBehaviorData(String header) {
        if (!StringUtils.hasText(header)) return null;
        try {
            byte[] json = Base64.getDecoder().decode(header);
            return MAPPER.readValue(json, BehaviorData.class);
        } catch (Exception e) {
            log.debug("Failed to decode behavior data: {}", e.getMessage());
            return null;
        }
    }

    private void persistLog(Long userId, String endpoint, String fingerprint,
                            BehaviorAnalysisService.BehaviorMetrics metrics,
                            OptionalDouble recaptchaScore, double finalScore,
                            RiskResult.Decision decision) {
        try {
            BehaviorLog logEntry = new BehaviorLog();
            logEntry.setUserId(userId);
            logEntry.setEndpoint(endpoint);
            logEntry.setFingerprint(fingerprint);
            logEntry.setMouseSpeedMean(metrics.mouseSpeedMean());
            logEntry.setMouseSpeedStdDev(metrics.mouseSpeedStdDev());
            logEntry.setStraightnessRatio(metrics.straightnessRatio());
            logEntry.setMouseIntervalStdDev(metrics.mouseIntervalStdDev());
            logEntry.setKeyIntervalStdDev(metrics.keyIntervalStdDev());
            logEntry.setRecaptchaScore(recaptchaScore.isPresent() ? recaptchaScore.getAsDouble() : null);
            logEntry.setFinalRiskScore(finalScore);
            logEntry.setDecision(decision.name());
            behaviorLogRepository.save(logEntry);
        } catch (Exception e) {
            log.warn("Failed to persist behavior log: {}", e.getMessage());
        }
    }
}
