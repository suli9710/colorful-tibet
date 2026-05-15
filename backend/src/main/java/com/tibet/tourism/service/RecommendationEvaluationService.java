package com.tibet.tourism.service;

import com.tibet.tourism.dto.RecommendationEvaluationResponse;
import com.tibet.tourism.entity.ScenicSpot;
import com.tibet.tourism.entity.UserVisitHistory;
import com.tibet.tourism.repository.ScenicSpotRepository;
import com.tibet.tourism.repository.UserVisitHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationEvaluationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationEvaluationService.class);
    private static final int DEFAULT_K = 10;
    private static final int MAX_K = 50;
    private static final int MAX_USER_LIMIT = 1000;

    @Autowired
    private UserVisitHistoryRepository historyRepository;

    @Autowired
    private ScenicSpotRepository spotRepository;

    public RecommendationEvaluationResponse evaluateItemBasedCf(int requestedK, int requestedUserLimit, boolean includeUserResults) {
        int k = Math.max(1, Math.min(requestedK <= 0 ? DEFAULT_K : requestedK, MAX_K));
        int userLimit = Math.max(1, Math.min(requestedUserLimit <= 0 ? MAX_USER_LIMIT : requestedUserLimit, MAX_USER_LIMIT));
        long startTime = System.currentTimeMillis();

        List<UserVisitHistory> histories = historyRepository.findAll();
        List<ScenicSpot> spots = spotRepository.findAll();
        Map<Long, ScenicSpot> spotsById = spots.stream()
                .filter(spot -> spot.getId() != null)
                .collect(Collectors.toMap(ScenicSpot::getId, spot -> spot, (left, right) -> left));

        Map<Long, List<UserVisitHistory>> historiesByUser = histories.stream()
                .filter(history -> history.getUser() != null
                        && history.getUser().getId() != null
                        && history.getSpot() != null
                        && history.getSpot().getId() != null)
                .collect(Collectors.groupingBy(history -> history.getUser().getId()));

        Map<Long, HoldoutSplit> splits = buildHoldoutSplits(historiesByUser, userLimit);
        Map<Long, Set<Long>> trainData = splits.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().trainSpotIds()));
        Map<Long, Map<Long, Double>> itemSimilarity = buildItemSimilarity(trainData);

        Set<Long> allRecommendedSpotIds = new HashSet<>();
        List<RecommendationEvaluationResponse.UserEvaluationEntry> userResults = new ArrayList<>();
        double precisionSum = 0.0;
        double recallSum = 0.0;
        double hitRateSum = 0.0;
        double ndcgSum = 0.0;
        int recommendationCountSum = 0;

        for (Map.Entry<Long, HoldoutSplit> entry : splits.entrySet()) {
            Long userId = entry.getKey();
            HoldoutSplit split = entry.getValue();
            List<Long> recommendations = recommendFromItemSimilarity(split.trainSpotIds(), itemSimilarity, k);
            allRecommendedSpotIds.addAll(recommendations);
            recommendationCountSum += recommendations.size();

            int rank = findRank(recommendations, split.heldOutSpotId());
            boolean hit = rank > 0;
            precisionSum += hit ? 1.0 / Math.max(1, recommendations.size()) : 0.0;
            recallSum += hit ? 1.0 : 0.0;
            hitRateSum += hit ? 1.0 : 0.0;
            ndcgSum += hit ? 1.0 / (Math.log(rank + 1) / Math.log(2)) : 0.0;

            if (includeUserResults) {
                userResults.add(buildUserResult(userId, split, recommendations, spotsById, rank, hit));
            }
        }

        int evaluatedUserCount = splits.size();
        RecommendationEvaluationResponse response = new RecommendationEvaluationResponse();
        response.setK(k);
        response.setEligibleUserCount(splits.size());
        response.setEvaluatedUserCount(evaluatedUserCount);
        response.setTotalSpotCount(spotsById.size());
        response.setRecommendedSpotCount(allRecommendedSpotIds.size());
        response.setPrecisionAtK(average(precisionSum, evaluatedUserCount));
        response.setRecallAtK(average(recallSum, evaluatedUserCount));
        response.setHitRateAtK(average(hitRateSum, evaluatedUserCount));
        response.setNdcgAtK(average(ndcgSum, evaluatedUserCount));
        response.setCoverage(spotsById.isEmpty() ? 0.0 : (double) allRecommendedSpotIds.size() / spotsById.size());
        response.setAverageRecommendationCount(average(recommendationCountSum, evaluatedUserCount));
        response.setEvaluatedAt(LocalDateTime.now());
        response.setUserResults(includeUserResults ? userResults : Collections.emptyList());

        logger.info("推荐离线评估完成: users={}, k={}, precision={}, recall={}, hitRate={}, durationMs={}",
                evaluatedUserCount,
                k,
                response.getPrecisionAtK(),
                response.getRecallAtK(),
                response.getHitRateAtK(),
                System.currentTimeMillis() - startTime);
        return response;
    }

    private Map<Long, HoldoutSplit> buildHoldoutSplits(Map<Long, List<UserVisitHistory>> historiesByUser, int userLimit) {
        Map<Long, HoldoutSplit> splits = new LinkedHashMap<>();
        historiesByUser.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    if (splits.size() >= userLimit) {
                        return;
                    }
                    List<UserVisitHistory> userHistories = entry.getValue().stream()
                            .filter(history -> history.getSpot() != null && history.getSpot().getId() != null)
                            .sorted(Comparator.comparing(
                                    UserVisitHistory::getVisitDate,
                                    Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                            .collect(Collectors.toList());
                    LinkedHashSet<Long> visitedSpotIds = userHistories.stream()
                            .map(history -> history.getSpot().getId())
                            .collect(Collectors.toCollection(LinkedHashSet::new));
                    if (visitedSpotIds.size() < 2) {
                        return;
                    }

                    Long heldOutSpotId = visitedSpotIds.iterator().next();
                    Set<Long> trainSpotIds = new LinkedHashSet<>(visitedSpotIds);
                    trainSpotIds.remove(heldOutSpotId);
                    splits.put(entry.getKey(), new HoldoutSplit(heldOutSpotId, trainSpotIds));
                });
        return splits;
    }

    private Map<Long, Map<Long, Double>> buildItemSimilarity(Map<Long, Set<Long>> trainData) {
        Map<Long, Integer> itemUserCounts = new HashMap<>();
        Map<Long, Map<Long, Integer>> coOccurrence = new HashMap<>();

        for (Set<Long> visitedSpotIds : trainData.values()) {
            List<Long> items = new ArrayList<>(visitedSpotIds);
            Collections.sort(items);
            for (Long itemId : items) {
                itemUserCounts.merge(itemId, 1, Integer::sum);
            }
            for (int i = 0; i < items.size() - 1; i++) {
                Long left = items.get(i);
                for (int j = i + 1; j < items.size(); j++) {
                    Long right = items.get(j);
                    coOccurrence.computeIfAbsent(left, key -> new HashMap<>()).merge(right, 1, Integer::sum);
                    coOccurrence.computeIfAbsent(right, key -> new HashMap<>()).merge(left, 1, Integer::sum);
                }
            }
        }

        Map<Long, Map<Long, Double>> similarity = new HashMap<>();
        for (Map.Entry<Long, Map<Long, Integer>> rowEntry : coOccurrence.entrySet()) {
            Long left = rowEntry.getKey();
            int leftCount = itemUserCounts.getOrDefault(left, 0);
            if (leftCount == 0) {
                continue;
            }
            for (Map.Entry<Long, Integer> scoreEntry : rowEntry.getValue().entrySet()) {
                Long right = scoreEntry.getKey();
                int rightCount = itemUserCounts.getOrDefault(right, 0);
                if (rightCount == 0) {
                    continue;
                }
                double score = scoreEntry.getValue() / Math.sqrt((double) leftCount * rightCount);
                if (score > 0.0) {
                    similarity.computeIfAbsent(left, key -> new HashMap<>()).put(right, score);
                }
            }
        }
        return similarity;
    }

    private List<Long> recommendFromItemSimilarity(Set<Long> trainSpotIds,
                                                   Map<Long, Map<Long, Double>> itemSimilarity,
                                                   int k) {
        Map<Long, Double> scores = new HashMap<>();
        for (Long visitedSpotId : trainSpotIds) {
            itemSimilarity.getOrDefault(visitedSpotId, Collections.emptyMap())
                    .forEach((candidateId, score) -> {
                        if (!trainSpotIds.contains(candidateId)) {
                            scores.merge(candidateId, score, Double::sum);
                        }
                    });
        }
        return scores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(k)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private RecommendationEvaluationResponse.UserEvaluationEntry buildUserResult(
            Long userId,
            HoldoutSplit split,
            List<Long> recommendations,
            Map<Long, ScenicSpot> spotsById,
            int rank,
            boolean hit) {
        RecommendationEvaluationResponse.UserEvaluationEntry userResult = new RecommendationEvaluationResponse.UserEvaluationEntry();
        userResult.setUserId(userId);
        userResult.setHeldOutSpotId(split.heldOutSpotId());
        userResult.setHeldOutSpotName(getSpotName(spotsById, split.heldOutSpotId()));
        userResult.setRank(rank);
        userResult.setHit(hit);
        userResult.setRecommendations(recommendations.stream()
                .map(spotId -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("spotId", spotId);
                    item.put("spotName", getSpotName(spotsById, spotId));
                    return item;
                })
                .collect(Collectors.toList()));
        return userResult;
    }

    private int findRank(List<Long> recommendations, Long targetSpotId) {
        for (int i = 0; i < recommendations.size(); i++) {
            if (Objects.equals(recommendations.get(i), targetSpotId)) {
                return i + 1;
            }
        }
        return 0;
    }

    private String getSpotName(Map<Long, ScenicSpot> spotsById, Long spotId) {
        ScenicSpot spot = spotsById.get(spotId);
        return spot == null ? null : spot.getName();
    }

    private double average(double sum, int count) {
        return count == 0 ? 0.0 : sum / count;
    }

    private record HoldoutSplit(Long heldOutSpotId, Set<Long> trainSpotIds) {
    }
}
