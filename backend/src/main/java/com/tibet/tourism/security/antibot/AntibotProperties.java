package com.tibet.tourism.security.antibot;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.antibot")
public class AntibotProperties {

    private boolean enabled = false;
    private Recaptcha recaptcha = new Recaptcha();
    private Fingerprint fingerprint = new Fingerprint();
    private Behavior behavior = new Behavior();
    private Risk risk = new Risk();

    public static class Recaptcha {
        private boolean enabled = false;
        private String siteKey = "";
        private String secretKey = "";
        private double minScore = 0.5;
        private String verifyUrl = "https://www.recaptcha.net/recaptcha/api/siteverify";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getSiteKey() { return siteKey; }
        public void setSiteKey(String siteKey) { this.siteKey = siteKey; }
        public String getSecretKey() { return secretKey; }
        public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
        public double getMinScore() { return minScore; }
        public void setMinScore(double minScore) { this.minScore = minScore; }
        public String getVerifyUrl() { return verifyUrl; }
        public void setVerifyUrl(String verifyUrl) { this.verifyUrl = verifyUrl; }
    }

    public static class Fingerprint {
        private boolean enabled = false;
        private int maxUsersPerFingerprint = 5;
        private int maxFingerprintsPerUser = 10;
        private long ttlSeconds = 86400;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public int getMaxUsersPerFingerprint() { return maxUsersPerFingerprint; }
        public void setMaxUsersPerFingerprint(int maxUsersPerFingerprint) { this.maxUsersPerFingerprint = maxUsersPerFingerprint; }
        public int getMaxFingerprintsPerUser() { return maxFingerprintsPerUser; }
        public void setMaxFingerprintsPerUser(int maxFingerprintsPerUser) { this.maxFingerprintsPerUser = maxFingerprintsPerUser; }
        public long getTtlSeconds() { return ttlSeconds; }
        public void setTtlSeconds(long ttlSeconds) { this.ttlSeconds = ttlSeconds; }
    }

    public static class Behavior {
        private boolean enabled = false;
        private double maxStraightnessRatio = 0.95;
        private double minSpeedStdDev = 0.5;
        private double minIntervalStdDev = 5.0;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public double getMaxStraightnessRatio() { return maxStraightnessRatio; }
        public void setMaxStraightnessRatio(double maxStraightnessRatio) { this.maxStraightnessRatio = maxStraightnessRatio; }
        public double getMinSpeedStdDev() { return minSpeedStdDev; }
        public void setMinSpeedStdDev(double minSpeedStdDev) { this.minSpeedStdDev = minSpeedStdDev; }
        public double getMinIntervalStdDev() { return minIntervalStdDev; }
        public void setMinIntervalStdDev(double minIntervalStdDev) { this.minIntervalStdDev = minIntervalStdDev; }
    }

    public static class Risk {
        private double recaptchaWeight = 0.3;
        private double behaviorWeight = 0.4;
        private double fingerprintWeight = 0.3;
        private int challengeThreshold = 60;
        private int blockThreshold = 85;

        public double getRecaptchaWeight() { return recaptchaWeight; }
        public void setRecaptchaWeight(double recaptchaWeight) { this.recaptchaWeight = recaptchaWeight; }
        public double getBehaviorWeight() { return behaviorWeight; }
        public void setBehaviorWeight(double behaviorWeight) { this.behaviorWeight = behaviorWeight; }
        public double getFingerprintWeight() { return fingerprintWeight; }
        public void setFingerprintWeight(double fingerprintWeight) { this.fingerprintWeight = fingerprintWeight; }
        public int getChallengeThreshold() { return challengeThreshold; }
        public void setChallengeThreshold(int challengeThreshold) { this.challengeThreshold = challengeThreshold; }
        public int getBlockThreshold() { return blockThreshold; }
        public void setBlockThreshold(int blockThreshold) { this.blockThreshold = blockThreshold; }
    }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Recaptcha getRecaptcha() { return recaptcha; }
    public void setRecaptcha(Recaptcha recaptcha) { this.recaptcha = recaptcha; }
    public Fingerprint getFingerprint() { return fingerprint; }
    public void setFingerprint(Fingerprint fingerprint) { this.fingerprint = fingerprint; }
    public Behavior getBehavior() { return behavior; }
    public void setBehavior(Behavior behavior) { this.behavior = behavior; }
    public Risk getRisk() { return risk; }
    public void setRisk(Risk risk) { this.risk = risk; }
}
