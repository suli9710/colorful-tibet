package com.tibet.tourism.service.recommendation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "recommendation")
public class RecommendationConfig {

    private int maxSimilarUsers = 15;
    private int maxResults = 10;
    private double minSimilarity = 0.05;
    private double defaultRating = 3.0;
    private double tagScoreMultiplier = 0.75;
    private double collaborativeWeight = 0.7;
    private double contentWeight = 0.3;
    private double diversityPenalty = 0.30;
    private double explorationRate = 0.1;
    private double minCommonItems = 2;
    private double exponentialDecayFactor = 0.95;
    private double clickWeight = 0.1;
    private double dwellWeight = 0.05;
    private double seasonalBoost = 1.2;
    private double contextWeight = 0.25;
    private double seasonalMatchBoost = 1.3;
    private double weatherMatchBoost = 1.2;
    private double distanceBoostFactor = 0.5;
    private double budgetPenalty = 0.8;
    private double companionMatchBoost = 1.15;
    private double userBasedWeight = 0.3;
    private double itemBasedWeight = 0.7;

    // --- getters and setters ---

    public int getMaxSimilarUsers() { return maxSimilarUsers; }
    public void setMaxSimilarUsers(int maxSimilarUsers) { this.maxSimilarUsers = maxSimilarUsers; }

    public int getMaxResults() { return maxResults; }
    public void setMaxResults(int maxResults) { this.maxResults = maxResults; }

    public double getMinSimilarity() { return minSimilarity; }
    public void setMinSimilarity(double minSimilarity) { this.minSimilarity = minSimilarity; }

    public double getDefaultRating() { return defaultRating; }
    public void setDefaultRating(double defaultRating) { this.defaultRating = defaultRating; }

    public double getTagScoreMultiplier() { return tagScoreMultiplier; }
    public void setTagScoreMultiplier(double tagScoreMultiplier) { this.tagScoreMultiplier = tagScoreMultiplier; }

    public double getCollaborativeWeight() { return collaborativeWeight; }
    public void setCollaborativeWeight(double collaborativeWeight) { this.collaborativeWeight = collaborativeWeight; }

    public double getContentWeight() { return contentWeight; }
    public void setContentWeight(double contentWeight) { this.contentWeight = contentWeight; }

    public double getDiversityPenalty() { return diversityPenalty; }
    public void setDiversityPenalty(double diversityPenalty) { this.diversityPenalty = diversityPenalty; }

    public double getExplorationRate() { return explorationRate; }
    public void setExplorationRate(double explorationRate) { this.explorationRate = explorationRate; }

    public double getMinCommonItems() { return minCommonItems; }
    public void setMinCommonItems(double minCommonItems) { this.minCommonItems = minCommonItems; }

    public double getExponentialDecayFactor() { return exponentialDecayFactor; }
    public void setExponentialDecayFactor(double exponentialDecayFactor) { this.exponentialDecayFactor = exponentialDecayFactor; }

    public double getClickWeight() { return clickWeight; }
    public void setClickWeight(double clickWeight) { this.clickWeight = clickWeight; }

    public double getDwellWeight() { return dwellWeight; }
    public void setDwellWeight(double dwellWeight) { this.dwellWeight = dwellWeight; }

    public double getSeasonalBoost() { return seasonalBoost; }
    public void setSeasonalBoost(double seasonalBoost) { this.seasonalBoost = seasonalBoost; }

    public double getContextWeight() { return contextWeight; }
    public void setContextWeight(double contextWeight) { this.contextWeight = contextWeight; }

    public double getSeasonalMatchBoost() { return seasonalMatchBoost; }
    public void setSeasonalMatchBoost(double seasonalMatchBoost) { this.seasonalMatchBoost = seasonalMatchBoost; }

    public double getWeatherMatchBoost() { return weatherMatchBoost; }
    public void setWeatherMatchBoost(double weatherMatchBoost) { this.weatherMatchBoost = weatherMatchBoost; }

    public double getDistanceBoostFactor() { return distanceBoostFactor; }
    public void setDistanceBoostFactor(double distanceBoostFactor) { this.distanceBoostFactor = distanceBoostFactor; }

    public double getBudgetPenalty() { return budgetPenalty; }
    public void setBudgetPenalty(double budgetPenalty) { this.budgetPenalty = budgetPenalty; }

    public double getCompanionMatchBoost() { return companionMatchBoost; }
    public void setCompanionMatchBoost(double companionMatchBoost) { this.companionMatchBoost = companionMatchBoost; }

    public double getUserBasedWeight() { return userBasedWeight; }
    public void setUserBasedWeight(double userBasedWeight) { this.userBasedWeight = userBasedWeight; }

    public double getItemBasedWeight() { return itemBasedWeight; }
    public void setItemBasedWeight(double itemBasedWeight) { this.itemBasedWeight = itemBasedWeight; }
}
