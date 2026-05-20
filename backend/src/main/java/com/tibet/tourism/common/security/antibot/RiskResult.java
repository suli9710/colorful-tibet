package com.tibet.tourism.common.security.antibot;

public record RiskResult(
        double recaptchaRisk,
        double behaviorRisk,
        double fingerprintRisk,
        double finalScore,
        Decision decision
) {
    public enum Decision { ALLOW, CHALLENGE, BLOCK }

    public static RiskResult allow() {
        return new RiskResult(0, 0, 0, 0, Decision.ALLOW);
    }
}
