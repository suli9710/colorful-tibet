package com.tibet.tourism.modules.recommendation.web.dto;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class RecommendationEvaluationResponse {
    private int k;
    private int eligibleUserCount;
    private int evaluatedUserCount;
    private int totalSpotCount;
    private int recommendedSpotCount;
    private double precisionAtK;
    private double recallAtK;
    private double hitRateAtK;
    private double ndcgAtK;
    private double coverage;
    private double averageRecommendationCount;
    private LocalDateTime evaluatedAt;
    private List<UserEvaluationEntry> userResults = Collections.emptyList();

    public int getK() {
        return k;
    }

    public void setK(int k) {
        this.k = k;
    }

    public int getEligibleUserCount() {
        return eligibleUserCount;
    }

    public void setEligibleUserCount(int eligibleUserCount) {
        this.eligibleUserCount = eligibleUserCount;
    }

    public int getEvaluatedUserCount() {
        return evaluatedUserCount;
    }

    public void setEvaluatedUserCount(int evaluatedUserCount) {
        this.evaluatedUserCount = evaluatedUserCount;
    }

    public int getTotalSpotCount() {
        return totalSpotCount;
    }

    public void setTotalSpotCount(int totalSpotCount) {
        this.totalSpotCount = totalSpotCount;
    }

    public int getRecommendedSpotCount() {
        return recommendedSpotCount;
    }

    public void setRecommendedSpotCount(int recommendedSpotCount) {
        this.recommendedSpotCount = recommendedSpotCount;
    }

    public double getPrecisionAtK() {
        return precisionAtK;
    }

    public void setPrecisionAtK(double precisionAtK) {
        this.precisionAtK = precisionAtK;
    }

    public double getRecallAtK() {
        return recallAtK;
    }

    public void setRecallAtK(double recallAtK) {
        this.recallAtK = recallAtK;
    }

    public double getHitRateAtK() {
        return hitRateAtK;
    }

    public void setHitRateAtK(double hitRateAtK) {
        this.hitRateAtK = hitRateAtK;
    }

    public double getNdcgAtK() {
        return ndcgAtK;
    }

    public void setNdcgAtK(double ndcgAtK) {
        this.ndcgAtK = ndcgAtK;
    }

    public double getCoverage() {
        return coverage;
    }

    public void setCoverage(double coverage) {
        this.coverage = coverage;
    }

    public double getAverageRecommendationCount() {
        return averageRecommendationCount;
    }

    public void setAverageRecommendationCount(double averageRecommendationCount) {
        this.averageRecommendationCount = averageRecommendationCount;
    }

    public LocalDateTime getEvaluatedAt() {
        return evaluatedAt;
    }

    public void setEvaluatedAt(LocalDateTime evaluatedAt) {
        this.evaluatedAt = evaluatedAt;
    }

    public List<UserEvaluationEntry> getUserResults() {
        return userResults;
    }

    public void setUserResults(List<UserEvaluationEntry> userResults) {
        this.userResults = userResults;
    }

    public static class UserEvaluationEntry {
        private Long userId;
        private Long heldOutSpotId;
        private String heldOutSpotName;
        private int rank;
        private boolean hit;
        private List<Map<String, Object>> recommendations = Collections.emptyList();

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Long getHeldOutSpotId() {
            return heldOutSpotId;
        }

        public void setHeldOutSpotId(Long heldOutSpotId) {
            this.heldOutSpotId = heldOutSpotId;
        }

        public String getHeldOutSpotName() {
            return heldOutSpotName;
        }

        public void setHeldOutSpotName(String heldOutSpotName) {
            this.heldOutSpotName = heldOutSpotName;
        }

        public int getRank() {
            return rank;
        }

        public void setRank(int rank) {
            this.rank = rank;
        }

        public boolean isHit() {
            return hit;
        }

        public void setHit(boolean hit) {
            this.hit = hit;
        }

        public List<Map<String, Object>> getRecommendations() {
            return recommendations;
        }

        public void setRecommendations(List<Map<String, Object>> recommendations) {
            this.recommendations = recommendations;
        }
    }
}
