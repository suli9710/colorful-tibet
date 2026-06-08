package com.tibet.tourism.modules.recommendation.application.strategy;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.domain.UserVisitHistory;
import com.tibet.tourism.modules.user.infra.UserVisitHistoryRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class UserBasedCFStrategy implements ScoringStrategy {

    private static final Logger logger = LoggerFactory.getLogger(UserBasedCFStrategy.class);

    private final UserVisitHistoryRepository historyRepository;
    private final RecommendationCacheService cacheService;
    private final RecommendationConfig config;

    public UserBasedCFStrategy(UserVisitHistoryRepository historyRepository,
                               RecommendationCacheService cacheService,
                               RecommendationConfig config) {
        this.historyRepository = historyRepository;
        this.cacheService = cacheService;
        this.config = config;
    }

    @Override
    public Map<Long, Double> score(Long userId, List<UserVisitHistory> visitHistory, Set<Long> visitedSpotIds) {
        List<Long> visitedSpotList = new ArrayList<>(visitedSpotIds);
        Map<Long, List<UserVisitHistory>> overlapHistoryByUser = historyRepository.findRecentBySpotIdIn(visitedSpotList)
                .stream()
                .filter(history -> !history.getUser().getId().equals(userId))
                .collect(Collectors.groupingBy(history -> history.getUser().getId()));

        Map<Long, Double> cachedSimilarities = cacheService.getSimilarity(userId);

        Map<Long, Double> userSimilarityMap = overlapHistoryByUser.entrySet().parallelStream()
                .map(entry -> {
                    Long otherUserId = entry.getKey();
                    if (cachedSimilarities != null && cachedSimilarities.containsKey(otherUserId)) {
                        return Map.entry(otherUserId, cachedSimilarities.get(otherUserId));
                    }
                    double similarity = calculateCombinedSimilarity(visitHistory, entry.getValue());
                    if (similarity < config.getMinSimilarity()) return null;
                    return Map.entry(otherUserId, similarity);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toConcurrentMap(Map.Entry::getKey, Map.Entry::getValue, (v1, v2) -> v1));

        logger.info("User-Based CF similar users found: count={}, minSimilarity={}",
                userSimilarityMap.size(), config.getMinSimilarity());

        cacheService.cacheSimilarity(userId, userSimilarityMap);

        if (userSimilarityMap.isEmpty()) return Collections.emptyMap();

        List<Long> similarUserIds = userSimilarityMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(config.getMaxSimilarUsers())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        Map<Long, List<UserVisitHistory>> similarUserHistories = historyRepository.findRecentByUserIdIn(similarUserIds)
                .stream()
                .collect(Collectors.groupingBy(history -> history.getUser().getId()));

        double currentUserAvg = calculateUserAverageRating(visitHistory);
        Map<Long, Double> scores = new HashMap<>();

        for (Long similarUserId : similarUserIds) {
            double similarity = userSimilarityMap.get(similarUserId);
            List<UserVisitHistory> histories = similarUserHistories.getOrDefault(similarUserId, Collections.emptyList());
            double similarUserAvg = calculateUserAverageRating(histories);

            for (UserVisitHistory history : histories) {
                Long spotId = history.getSpot().getId();
                if (spotId == null || visitedSpotIds.contains(spotId)) continue;

                double rawRating = normalizeRating(history.getRating());
                double adjustedRating = Math.max(1.0, Math.min(5.0, rawRating - similarUserAvg + currentUserAvg));
                double recencyBoost = calculateRecencyBoost(history.getVisitDate());
                double engagementWeight = calculateEngagementWeight(history.getClickCount(), history.getDwellSeconds());
                double score = similarity * (adjustedRating + recencyBoost + engagementWeight);
                scores.merge(spotId, score, Double::sum);
            }
        }

        return scores;
    }

    private double calculateCombinedSimilarity(List<UserVisitHistory> user1History, List<UserVisitHistory> user2History) {
        Map<Long, Double> user1Ratings = toRatingMap(user1History);
        Map<Long, Double> user2Ratings = toRatingMap(user2History);

        Set<Long> commonSpots = new HashSet<>(user1Ratings.keySet());
        commonSpots.retainAll(user2Ratings.keySet());

        if (commonSpots.size() < config.getMinCommonItems()) return 0.0;

        double adjustedCosine = calculateAdjustedCosineSimilarity(user1History, user2History, commonSpots);
        double jaccard = calculateJaccardSimilarity(user1Ratings.keySet(), user2Ratings.keySet());
        double timeWeighted = calculateTimeWeightedSimilarity(user1History, user2History, commonSpots);

        return 0.6 * adjustedCosine + 0.2 * jaccard + 0.2 * timeWeighted;
    }

    private double calculateAdjustedCosineSimilarity(List<UserVisitHistory> user1History,
                                                     List<UserVisitHistory> user2History,
                                                     Set<Long> commonSpots) {
        if (commonSpots.isEmpty()) return 0.0;

        double user1Avg = user1History.stream().mapToDouble(h -> normalizeRating(h.getRating())).average().orElse(config.getDefaultRating());
        double user2Avg = user2History.stream().mapToDouble(h -> normalizeRating(h.getRating())).average().orElse(config.getDefaultRating());

        Map<Long, Double> user1Ratings = toRatingMap(user1History);
        Map<Long, Double> user2Ratings = toRatingMap(user2History);

        double numerator = 0.0, sumSq1 = 0.0, sumSq2 = 0.0;
        for (Long spotId : commonSpots) {
            double diff1 = user1Ratings.get(spotId) - user1Avg;
            double diff2 = user2Ratings.get(spotId) - user2Avg;
            numerator += diff1 * diff2;
            sumSq1 += diff1 * diff1;
            sumSq2 += diff2 * diff2;
        }
        double denominator = Math.sqrt(sumSq1) * Math.sqrt(sumSq2);
        return denominator == 0 ? 0.0 : numerator / denominator;
    }

    private double calculateJaccardSimilarity(Set<Long> set1, Set<Long> set2) {
        Set<Long> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        Set<Long> union = new HashSet<>(set1);
        union.addAll(set2);
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private double calculateTimeWeightedSimilarity(List<UserVisitHistory> user1History,
                                                   List<UserVisitHistory> user2History,
                                                   Set<Long> commonSpots) {
        if (commonSpots.isEmpty()) return 0.0;

        Map<Long, LocalDateTime> user1Times = user1History.stream()
                .filter(h -> commonSpots.contains(h.getSpot().getId()))
                .collect(Collectors.toMap(h -> h.getSpot().getId(),
                        h -> h.getVisitDate() != null ? h.getVisitDate() : LocalDateTime.now(),
                        (a, b) -> a.isAfter(b) ? a : b));

        Map<Long, LocalDateTime> user2Times = user2History.stream()
                .filter(h -> commonSpots.contains(h.getSpot().getId()))
                .collect(Collectors.toMap(h -> h.getSpot().getId(),
                        h -> h.getVisitDate() != null ? h.getVisitDate() : LocalDateTime.now(),
                        (a, b) -> a.isAfter(b) ? a : b));

        double weightSum = 0.0;
        int counted = 0;
        for (Long spotId : commonSpots) {
            LocalDateTime time1 = user1Times.get(spotId);
            LocalDateTime time2 = user2Times.get(spotId);
            if (time1 == null || time2 == null) continue;
            long daysDiff = Math.abs(Duration.between(time1, time2).toDays());
            weightSum += Math.pow(config.getExponentialDecayFactor(), daysDiff / 30.0);
            counted++;
        }
        return counted == 0 ? 0.0 : weightSum / counted;
    }

    private Map<Long, Double> toRatingMap(List<UserVisitHistory> histories) {
        return histories.stream()
                .collect(Collectors.toMap(
                        h -> h.getSpot().getId(),
                        h -> normalizeRating(h.getRating()),
                        (left, right) -> Math.max(left, right)));
    }

    private double normalizeRating(Integer rating) {
        return rating == null ? config.getDefaultRating() : rating.doubleValue();
    }

    private double calculateUserAverageRating(List<UserVisitHistory> histories) {
        if (histories.isEmpty()) return config.getDefaultRating();
        return histories.stream().mapToDouble(h -> normalizeRating(h.getRating())).average().orElse(config.getDefaultRating());
    }

    private double calculateRecencyBoost(LocalDateTime visitDate) {
        if (visitDate == null) return 0.0;
        long days = Math.max(0, Duration.between(visitDate, LocalDateTime.now()).toDays());
        double exponentialDecay = Math.pow(config.getExponentialDecayFactor(), days / 30.0);
        int month = visitDate.getMonthValue();
        double seasonalBoost = (month >= 5 && month <= 10) ? config.getSeasonalBoost() : 1.0;
        return exponentialDecay * seasonalBoost;
    }

    private double calculateEngagementWeight(Integer clickCount, Integer dwellSeconds) {
        double clickW = clickCount == null ? 0.0 : Math.log1p(clickCount) * config.getClickWeight();
        double dwellW = dwellSeconds == null ? 0.0 : Math.min(dwellSeconds / 60.0, 5.0) * config.getDwellWeight();
        return clickW + dwellW;
    }
}
