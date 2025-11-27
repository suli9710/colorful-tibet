package com.tibet.tourism.dto;

import com.tibet.tourism.entity.ScenicSpot;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class RecommendationDebugResponse {

    private Long userId;
    private boolean hasHistory;
    private boolean fallbackUsed;
    private List<HistoryEntry> history;
    private List<SimilarUserEntry> similarUsers;
    private List<CandidateScoreEntry> candidateScores;
    private Map<String, Double> tagProfile;
    private List<ScenicSpot> recommendations;
    private Map<Long, String> recommendationReasons; // 推荐原因：spotId -> reason

    public Map<Long, String> getRecommendationReasons() {
        return recommendationReasons;
    }

    public void setRecommendationReasons(Map<Long, String> recommendationReasons) {
        this.recommendationReasons = recommendationReasons;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public boolean isHasHistory() {
        return hasHistory;
    }

    public void setHasHistory(boolean hasHistory) {
        this.hasHistory = hasHistory;
    }

    public boolean isFallbackUsed() {
        return fallbackUsed;
    }

    public void setFallbackUsed(boolean fallbackUsed) {
        this.fallbackUsed = fallbackUsed;
    }

    public List<HistoryEntry> getHistory() {
        return history;
    }

    public void setHistory(List<HistoryEntry> history) {
        this.history = history;
    }

    public List<SimilarUserEntry> getSimilarUsers() {
        return similarUsers;
    }

    public void setSimilarUsers(List<SimilarUserEntry> similarUsers) {
        this.similarUsers = similarUsers;
    }

    public List<CandidateScoreEntry> getCandidateScores() {
        return candidateScores;
    }

    public void setCandidateScores(List<CandidateScoreEntry> candidateScores) {
        this.candidateScores = candidateScores;
    }

    public Map<String, Double> getTagProfile() {
        return tagProfile;
    }

    public void setTagProfile(Map<String, Double> tagProfile) {
        this.tagProfile = tagProfile;
    }

    public List<ScenicSpot> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<ScenicSpot> recommendations) {
        this.recommendations = recommendations;
    }

    public static class HistoryEntry {
        private Long spotId;
        private String spotName;
        private Double rating;
        private LocalDateTime visitDate;

        public Long getSpotId() {
            return spotId;
        }

        public void setSpotId(Long spotId) {
            this.spotId = spotId;
        }

        public String getSpotName() {
            return spotName;
        }

        public void setSpotName(String spotName) {
            this.spotName = spotName;
        }

        public Double getRating() {
            return rating;
        }

        public void setRating(Double rating) {
            this.rating = rating;
        }

        public LocalDateTime getVisitDate() {
            return visitDate;
        }

        public void setVisitDate(LocalDateTime visitDate) {
            this.visitDate = visitDate;
        }
    }

    public static class SimilarUserEntry {
        private Long userId;
        private Double similarity;
        private Double adjustedCosine; // 调整后的余弦相似度
        private Double jaccard; // Jaccard相似度
        private Double timeWeighted; // 时间加权相似度
        private Integer commonSpotsCount; // 共同访问景点数

        public Long getUserId() {
            return userId;
        }

        public void setUserId(Long userId) {
            this.userId = userId;
        }

        public Double getSimilarity() {
            return similarity;
        }

        public void setSimilarity(Double similarity) {
            this.similarity = similarity;
        }

        public Double getAdjustedCosine() {
            return adjustedCosine;
        }

        public void setAdjustedCosine(Double adjustedCosine) {
            this.adjustedCosine = adjustedCosine;
        }

        public Double getJaccard() {
            return jaccard;
        }

        public void setJaccard(Double jaccard) {
            this.jaccard = jaccard;
        }

        public Double getTimeWeighted() {
            return timeWeighted;
        }

        public void setTimeWeighted(Double timeWeighted) {
            this.timeWeighted = timeWeighted;
        }

        public Integer getCommonSpotsCount() {
            return commonSpotsCount;
        }

        public void setCommonSpotsCount(Integer commonSpotsCount) {
            this.commonSpotsCount = commonSpotsCount;
        }
    }

    public static class CandidateScoreEntry {
        private Long spotId;
        private String spotName;
        private Double collaborativeScore;
        private Double tagScore;
        private Double finalScore;
        private Double normalizedCollaborativeScore; // 归一化后的协同得分
        private Double engagementScore; // 行为得分（点击+停留）
        private Double recencyBoost; // 时间衰减增强
        private Double diversityPenalty; // 多样性惩罚

        public Long getSpotId() {
            return spotId;
        }

        public void setSpotId(Long spotId) {
            this.spotId = spotId;
        }

        public String getSpotName() {
            return spotName;
        }

        public void setSpotName(String spotName) {
            this.spotName = spotName;
        }

        public Double getCollaborativeScore() {
            return collaborativeScore;
        }

        public void setCollaborativeScore(Double collaborativeScore) {
            this.collaborativeScore = collaborativeScore;
        }

        public Double getTagScore() {
            return tagScore;
        }

        public void setTagScore(Double tagScore) {
            this.tagScore = tagScore;
        }

        public Double getFinalScore() {
            return finalScore;
        }

        public void setFinalScore(Double finalScore) {
            this.finalScore = finalScore;
        }

        public Double getNormalizedCollaborativeScore() {
            return normalizedCollaborativeScore;
        }

        public void setNormalizedCollaborativeScore(Double normalizedCollaborativeScore) {
            this.normalizedCollaborativeScore = normalizedCollaborativeScore;
        }

        public Double getEngagementScore() {
            return engagementScore;
        }

        public void setEngagementScore(Double engagementScore) {
            this.engagementScore = engagementScore;
        }

        public Double getRecencyBoost() {
            return recencyBoost;
        }

        public void setRecencyBoost(Double recencyBoost) {
            this.recencyBoost = recencyBoost;
        }

        public Double getDiversityPenalty() {
            return diversityPenalty;
        }

        public void setDiversityPenalty(Double diversityPenalty) {
            this.diversityPenalty = diversityPenalty;
        }
    }
    
    // 新增：算法配置信息
    private Map<String, Object> algorithmConfig;
    private Long computationTimeMs; // 计算耗时（毫秒）

    public Map<String, Object> getAlgorithmConfig() {
        return algorithmConfig;
    }

    public void setAlgorithmConfig(Map<String, Object> algorithmConfig) {
        this.algorithmConfig = algorithmConfig;
    }

    public Long getComputationTimeMs() {
        return computationTimeMs;
    }

    public void setComputationTimeMs(Long computationTimeMs) {
        this.computationTimeMs = computationTimeMs;
    }
}













