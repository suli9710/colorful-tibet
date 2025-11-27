package com.tibet.tourism.service;

import com.tibet.tourism.dto.RecommendationDebugResponse;
import com.tibet.tourism.dto.RecommendationDebugResponse.CandidateScoreEntry;
import com.tibet.tourism.dto.RecommendationDebugResponse.HistoryEntry;
import com.tibet.tourism.dto.RecommendationDebugResponse.SimilarUserEntry;
import com.tibet.tourism.entity.ScenicSpot;
import com.tibet.tourism.entity.SpotTag;
import com.tibet.tourism.entity.UserVisitHistory;
import com.tibet.tourism.repository.ScenicSpotRepository;
import com.tibet.tourism.repository.SpotTagRepository;
import com.tibet.tourism.repository.UserVisitHistoryRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    // 基础配置常量
    private static final int MAX_SIMILAR_USERS = 15; // 增加相似用户数量以提高召回率
    private static final int MAX_RESULTS = 10;
    private static final double MIN_SIMILARITY = 0.05; // 降低阈值以增加召回
    @SuppressWarnings("unused")
    private static final double RECENCY_WINDOW_DAYS = 365d; // 扩展时间窗口（保留用于未来扩展）
    private static final double DEFAULT_RATING = 3d;
    private static final double TAG_SCORE_MULTIPLIER = 0.75d;
    
    // 新增优化参数
    private static final double COLLABORATIVE_WEIGHT = 0.7d; // 协同过滤权重
    private static final double CONTENT_WEIGHT = 0.3d; // 内容过滤权重
    private static final double DIVERSITY_PENALTY = 0.15d; // 多样性惩罚系数
    private static final double EXPLORATION_RATE = 0.1d; // 探索率（ε-greedy）
    private static final double MIN_COMMON_ITEMS = 2; // 最小共同访问景点数
    private static final double EXPONENTIAL_DECAY_FACTOR = 0.95d; // 指数衰减因子
    private static final double CLICK_WEIGHT = 0.1d; // 点击权重
    private static final double DWELL_WEIGHT = 0.05d; // 停留时间权重（秒）
    private static final double SEASONAL_BOOST = 1.2d; // 季节性增强

    @Autowired
    private UserVisitHistoryRepository historyRepository;

    @Autowired
    private ScenicSpotRepository spotRepository;

    @Autowired
    private SpotTagRepository spotTagRepository;
    
    // 缓存：用户相似度映射（可扩展为Redis缓存）
    private final Map<Long, Map<Long, Double>> similarityCache = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Double>> tagProfileCache = new ConcurrentHashMap<>();
    private static final int CACHE_SIZE_LIMIT = 1000; // 缓存大小限制

    public List<ScenicSpot> recommendSpotsForUser(Long userId) {
        RecommendationComputationContext context = computeContext(userId);
        return context.getRecommendations();
    }

    public RecommendationDebugResponse recommendWithDebug(Long userId) {
        long startTime = System.currentTimeMillis();
        
        RecommendationComputationContext context = computeContext(userId);
        
        RecommendationDebugResponse response = new RecommendationDebugResponse();
        response.setUserId(userId);
        response.setHasHistory(context.hasHistory());
        response.setFallbackUsed(context.isFallbackUsed());
        response.setRecommendations(context.getRecommendations());
        response.setTagProfile(context.getTagProfile());
        response.setHistory(buildHistoryEntries(context.getCurrentUserHistory()));
        response.setSimilarUsers(buildSimilarUserEntries(context, context.getUserSimilarityMap()));
        response.setCandidateScores(buildCandidateEntries(context));
        
        // 设置算法配置信息
        Map<String, Object> config = new HashMap<>();
        config.put("maxSimilarUsers", MAX_SIMILAR_USERS);
        config.put("maxResults", MAX_RESULTS);
        config.put("minSimilarity", MIN_SIMILARITY);
        config.put("collaborativeWeight", COLLABORATIVE_WEIGHT);
        config.put("contentWeight", CONTENT_WEIGHT);
        config.put("diversityPenalty", DIVERSITY_PENALTY);
        config.put("explorationRate", EXPLORATION_RATE);
        response.setAlgorithmConfig(config);
        
        // 生成推荐原因
        Map<Long, String> recommendationReasons = generateRecommendationReasons(context);
        response.setRecommendationReasons(recommendationReasons);
        
        // 计算耗时
        long endTime = System.currentTimeMillis();
        response.setComputationTimeMs(endTime - startTime);
        
        return response;
    }
    
    /**
     * 生成推荐原因
     */
    private Map<Long, String> generateRecommendationReasons(RecommendationComputationContext context) {
        Map<Long, String> reasons = new HashMap<>();
        List<ScenicSpot> recommendations = context.getRecommendations();
        
        if (recommendations == null || recommendations.isEmpty()) {
            return reasons;
        }
        
        boolean fallbackUsed = context.isFallbackUsed();
        Map<Long, Double> collaborativeScores = context.getCollaborativeScores();
        Map<Long, Double> tagScores = context.getTagScores();
        
        for (ScenicSpot spot : recommendations) {
            Long spotId = spot.getId();
            if (spotId == null) continue;
            
            List<String> reasonParts = new ArrayList<>();
            
            if (fallbackUsed) {
                // 冷启动情况：基于热门度和评分
                if (spot.getVisitCount() != null && spot.getVisitCount() > 15000) {
                    reasonParts.add("热门景点");
                }
                if (spot.getRating() != null && spot.getRating().doubleValue() >= 4.0) {
                    reasonParts.add("高评分");
                }
                if (reasonParts.isEmpty()) {
                    reasonParts.add("值得探索");
                }
            } else {
                // 有历史记录：基于协同过滤和内容过滤
                double collaborativeScore = collaborativeScores.getOrDefault(spotId, 0.0);
                double tagScore = tagScores.getOrDefault(spotId, 0.0);
                
                // 降低阈值，确保能生成推荐原因
                if (collaborativeScore > 0.1) {
                    reasonParts.add("相似用户也喜欢");
                }
                if (tagScore > 0.05) {
                    reasonParts.add("符合您的兴趣");
                }
                if (spot.getVisitCount() != null && spot.getVisitCount() > 15000) {
                    reasonParts.add("热门景点");
                }
                if (spot.getRating() != null && spot.getRating().doubleValue() >= 4.0) {
                    reasonParts.add("高评分");
                }
                
                // 如果都没有，使用默认原因
                if (reasonParts.isEmpty()) {
                    reasonParts.add("为您精心挑选");
                }
            }
            
            reasons.put(spotId, String.join("、", reasonParts));
        }
        
        return reasons;
    }

    private Map<String, Double> buildUserTagProfile(List<UserVisitHistory> histories, Set<Long> visitedSpotIds) {
        if (histories.isEmpty() || visitedSpotIds.isEmpty()) {
            return Collections.emptyMap();
        }

        List<SpotTag> spotTags = spotTagRepository.findBySpotIdIn(visitedSpotIds);
        if (spotTags.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, List<String>> tagsBySpot = spotTags.stream()
                .filter(tag -> tag.getSpot() != null && tag.getSpot().getId() != null)
                .collect(Collectors.groupingBy(tag -> tag.getSpot().getId(),
                        Collectors.mapping(SpotTag::getTag, Collectors.toList())));

        Map<String, Double> tagWeights = new HashMap<>();
        for (UserVisitHistory history : histories) {
            Long spotId = history.getSpot().getId();
            List<String> tags = tagsBySpot.get(spotId);
            if (tags == null || tags.isEmpty()) {
                continue;
            }
            // 增强权重计算：评分 + 时间衰减 + 行为权重
            double ratingWeight = normalizeRating(history.getRating());
            double recencyWeight = calculateRecencyBoost(history.getVisitDate());
            double engagementWeight = calculateEngagementWeight(history.getClickCount(), history.getDwellSeconds());
            double weight = ratingWeight + recencyWeight + engagementWeight;
            
            for (String tagValue : tags) {
                if (tagValue == null) {
                    continue;
                }
                tagWeights.merge(tagValue, weight, (existing, addition) -> existing + addition);
            }
        }
        return tagWeights;
    }

    /**
     * 使用并行流优化标签评分计算
     */
    private Map<Long, Double> scoreSpotsByTags(Map<String, Double> tagProfile, Set<Long> visitedSpotIds) {
        if (tagProfile.isEmpty()) {
            return Collections.emptyMap();
        }

        List<ScenicSpot> allSpots = spotRepository.findAll();
        
        // 使用并行流处理以提高性能
        return allSpots.parallelStream()
                .filter(spot -> spot.getId() != null && !visitedSpotIds.contains(spot.getId()))
                .filter(spot -> spot.getTags() != null && !spot.getTags().isEmpty())
                .collect(Collectors.toConcurrentMap(
                    ScenicSpot::getId,
                    spot -> {
                        List<String> spotTags = extractTagValues(spot.getTags());
                        return spotTags.stream()
                                .mapToDouble(tag -> tagProfile.getOrDefault(tag, 0.0))
                                .sum();
                    },
                    (v1, v2) -> v1 + v2
                ))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 0.0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private List<String> extractTagValues(List<SpotTag> tags) {
        if (tags == null) {
            return Collections.emptyList();
        }
        return tags.stream()
                .map(SpotTag::getTag)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 相似度详细信息类
     */
    private static class SimilarityDetails {
        private double adjustedCosine;
        private double jaccard;
        private double timeWeighted;
        private int commonSpotsCount;
        
        public double getAdjustedCosine() { return adjustedCosine; }
        public void setAdjustedCosine(double adjustedCosine) { this.adjustedCosine = adjustedCosine; }
        public double getJaccard() { return jaccard; }
        public void setJaccard(double jaccard) { this.jaccard = jaccard; }
        public double getTimeWeighted() { return timeWeighted; }
        public void setTimeWeighted(double timeWeighted) { this.timeWeighted = timeWeighted; }
        public int getCommonSpotsCount() { return commonSpotsCount; }
        public void setCommonSpotsCount(int commonSpotsCount) { this.commonSpotsCount = commonSpotsCount; }
    }
    
    /**
     * 计算用户相似度 - 使用多种相似度度量方法的混合
     * 1. 调整后的余弦相似度（考虑用户评分偏差）
     * 2. Jaccard相似度（基于共同访问集合）
     * 3. 时间加权相似度
     * @deprecated 使用 calculateSimilarityWithDetails 替代以获取详细信息
     */
    @SuppressWarnings("unused")
    private double calculateSimilarity(List<UserVisitHistory> user1History, List<UserVisitHistory> user2History) {
        Map<Long, Double> user1Ratings = toRatingMap(user1History);
        Map<Long, Double> user2Ratings = toRatingMap(user2History);

        Set<Long> commonSpots = new HashSet<>(user1Ratings.keySet());
        commonSpots.retainAll(user2Ratings.keySet());

        if (commonSpots.size() < MIN_COMMON_ITEMS) return 0.0;

        // 1. 调整后的余弦相似度（Adjusted Cosine Similarity）
        double adjustedCosine = calculateAdjustedCosineSimilarity(user1History, user2History, commonSpots);
        
        // 2. Jaccard相似度（基于访问集合）
        double jaccard = calculateJaccardSimilarity(user1Ratings.keySet(), user2Ratings.keySet());
        
        // 3. 时间加权相似度
        double timeWeighted = calculateTimeWeightedSimilarity(user1History, user2History, commonSpots);
        
        // 加权组合：调整余弦(60%) + Jaccard(20%) + 时间加权(20%)
        return 0.6 * adjustedCosine + 0.2 * jaccard + 0.2 * timeWeighted;
    }
    
    /**
     * 计算用户相似度并返回详细信息
     */
    private SimilarityDetails calculateSimilarityWithDetails(List<UserVisitHistory> user1History, List<UserVisitHistory> user2History) {
        SimilarityDetails details = new SimilarityDetails();
        Map<Long, Double> user1Ratings = toRatingMap(user1History);
        Map<Long, Double> user2Ratings = toRatingMap(user2History);

        Set<Long> commonSpots = new HashSet<>(user1Ratings.keySet());
        commonSpots.retainAll(user2Ratings.keySet());
        
        details.setCommonSpotsCount(commonSpots.size());

        if (commonSpots.size() < MIN_COMMON_ITEMS) {
            return details;
        }

        double adjustedCosine = calculateAdjustedCosineSimilarity(user1History, user2History, commonSpots);
        double jaccard = calculateJaccardSimilarity(user1Ratings.keySet(), user2Ratings.keySet());
        double timeWeighted = calculateTimeWeightedSimilarity(user1History, user2History, commonSpots);
        
        details.setAdjustedCosine(adjustedCosine);
        details.setJaccard(jaccard);
        details.setTimeWeighted(timeWeighted);
        
        return details;
    }
    
    /**
     * 调整后的余弦相似度 - 考虑用户平均评分偏差
     */
    private double calculateAdjustedCosineSimilarity(List<UserVisitHistory> user1History, 
                                                      List<UserVisitHistory> user2History,
                                                      Set<Long> commonSpots) {
        if (commonSpots.isEmpty()) return 0.0;
        
        // 计算用户平均评分
        double user1Avg = user1History.stream()
                .mapToDouble(h -> normalizeRating(h.getRating()))
                .average()
                .orElse(DEFAULT_RATING);
        
        double user2Avg = user2History.stream()
                .mapToDouble(h -> normalizeRating(h.getRating()))
                .average()
                .orElse(DEFAULT_RATING);
        
        Map<Long, Double> user1Ratings = toRatingMap(user1History);
        Map<Long, Double> user2Ratings = toRatingMap(user2History);
        
        double numerator = 0.0;
        double sumSq1 = 0.0;
        double sumSq2 = 0.0;
        
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
    
    /**
     * Jaccard相似度 - 基于共同访问的景点比例
     */
    private double calculateJaccardSimilarity(Set<Long> set1, Set<Long> set2) {
        Set<Long> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<Long> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    /**
     * 时间加权相似度 - 考虑访问时间的接近程度
     */
    private double calculateTimeWeightedSimilarity(List<UserVisitHistory> user1History,
                                                   List<UserVisitHistory> user2History,
                                                   Set<Long> commonSpots) {
        if (commonSpots.isEmpty()) return 0.0;
        
        Map<Long, LocalDateTime> user1VisitTimes = user1History.stream()
                .filter(h -> commonSpots.contains(h.getSpot().getId()))
                .collect(Collectors.toMap(
                    h -> h.getSpot().getId(),
                    h -> h.getVisitDate() != null ? h.getVisitDate() : LocalDateTime.now(),
                    (a, b) -> a.isAfter(b) ? a : b
                ));
        
        Map<Long, LocalDateTime> user2VisitTimes = user2History.stream()
                .filter(h -> commonSpots.contains(h.getSpot().getId()))
                .collect(Collectors.toMap(
                    h -> h.getSpot().getId(),
                    h -> h.getVisitDate() != null ? h.getVisitDate() : LocalDateTime.now(),
                    (a, b) -> a.isAfter(b) ? a : b
                ));
        
        double totalWeight = 0.0;
        double weightedSum = 0.0;
        
        for (Long spotId : commonSpots) {
            LocalDateTime time1 = user1VisitTimes.get(spotId);
            LocalDateTime time2 = user2VisitTimes.get(spotId);
            if (time1 == null || time2 == null) continue;
            
            long daysDiff = Math.abs(Duration.between(time1, time2).toDays());
            // 时间越接近，权重越高（指数衰减）
            double timeWeight = Math.pow(EXPONENTIAL_DECAY_FACTOR, daysDiff / 30.0);
            totalWeight += timeWeight;
            weightedSum += timeWeight;
        }
        
        return totalWeight == 0 ? 0.0 : weightedSum / (totalWeight * commonSpots.size());
    }

    private Map<Long, Double> toRatingMap(List<UserVisitHistory> histories) {
        return histories.stream()
                .collect(Collectors.toMap(
                        history -> history.getSpot().getId(),
                        history -> normalizeRating(history.getRating()),
                        (left, right) -> Math.max(left, right)
                ));
    }

    /**
     * 归一化评分 - 考虑用户评分偏差
     */
    private double normalizeRating(Integer rating) {
        return rating == null ? DEFAULT_RATING : rating.doubleValue();
    }
    
    /**
     * 计算用户平均评分（用于偏差调整）
     */
    private double calculateUserAverageRating(List<UserVisitHistory> histories) {
        if (histories.isEmpty()) return DEFAULT_RATING;
        return histories.stream()
                .mapToDouble(h -> normalizeRating(h.getRating()))
                .average()
                .orElse(DEFAULT_RATING);
    }

    /**
     * 优化的时间衰减 - 使用指数衰减而非线性衰减
     * 同时考虑季节性因素
     */
    private double calculateRecencyBoost(LocalDateTime visitDate) {
        if (visitDate == null) {
            return 0.0;
        }
        long days = Math.max(0, Duration.between(visitDate, LocalDateTime.now()).toDays());
        
        // 指数衰减：越近期的访问权重越高
        double exponentialDecay = Math.pow(EXPONENTIAL_DECAY_FACTOR, days / 30.0);
        
        // 季节性增强：如果访问时间在旅游旺季，给予额外权重
        double seasonalBoost = calculateSeasonalBoost(visitDate);
        
        return exponentialDecay * seasonalBoost;
    }
    
    /**
     * 季节性增强 - 根据访问时间判断是否在旅游旺季
     */
    private double calculateSeasonalBoost(LocalDateTime visitDate) {
        if (visitDate == null) return 1.0;
        
        int month = visitDate.getMonthValue();
        // 西藏旅游旺季：5-10月（春季到秋季）
        if (month >= 5 && month <= 10) {
            return SEASONAL_BOOST;
        }
        return 1.0;
    }
    
    /**
     * 计算行为权重 - 结合点击次数和停留时间
     */
    private double calculateEngagementWeight(Integer clickCount, Integer dwellSeconds) {
        double clickWeight = clickCount == null ? 0.0 : Math.log1p(clickCount) * CLICK_WEIGHT;
        double dwellWeight = dwellSeconds == null ? 0.0 : Math.min(dwellSeconds / 60.0, 5.0) * DWELL_WEIGHT;
        return clickWeight + dwellWeight;
    }

    private List<ScenicSpot> fallbackPopularSpots() {
        List<UserVisitHistory> histories = historyRepository.findAll();
        if (histories.isEmpty()) {
            return spotRepository.findAll().stream()
                    .limit(MAX_RESULTS)
                    .collect(Collectors.toList());
        }

        Map<Long, Double> spotScores = histories.stream()
                .collect(Collectors.groupingBy(
                        history -> history.getSpot().getId(),
                        Collectors.summingDouble(history -> normalizeRating(history.getRating()) + calculateRecencyBoost(history.getVisitDate()))
                ));

        return spotScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(MAX_RESULTS)
                .map(entry -> {
                    Long spotId = Objects.requireNonNull(entry.getKey());
                    return spotRepository.findById(spotId).orElse(null);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private RecommendationComputationContext computeContext(Long userId) {
        RecommendationComputationContext context = new RecommendationComputationContext();

        List<UserVisitHistory> currentUserHistory = historyRepository.findByUserId(userId);
        context.setCurrentUserHistory(currentUserHistory);
        context.setHasHistory(!currentUserHistory.isEmpty());

        if (currentUserHistory.isEmpty()) {
            context.setFallbackUsed(true);
            context.setRecommendations(fallbackPopularSpots());
            return context;
        }

        Set<Long> visitedSpotIds = currentUserHistory.stream()
                .map(history -> history.getSpot().getId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (visitedSpotIds.isEmpty()) {
            context.setFallbackUsed(true);
            context.setRecommendations(fallbackPopularSpots());
            return context;
        }

        // 尝试从缓存获取标签画像
        Map<String, Double> tagPreferenceProfile = tagProfileCache.get(userId);
        if (tagPreferenceProfile == null) {
            tagPreferenceProfile = buildUserTagProfile(currentUserHistory, visitedSpotIds);
            // 更新缓存
            if (tagProfileCache.size() < CACHE_SIZE_LIMIT) {
                tagProfileCache.put(userId, tagPreferenceProfile);
            }
        }
        context.setTagProfile(tagPreferenceProfile);

        List<Long> visitedSpotList = new ArrayList<>(visitedSpotIds);
        Map<Long, List<UserVisitHistory>> overlapHistoryByUser = historyRepository.findBySpotIdIn(visitedSpotList)
                .stream()
                .filter(history -> !history.getUser().getId().equals(userId))
                .collect(Collectors.groupingBy(history -> history.getUser().getId()));

        // 使用并行流计算相似度以提高性能，同时收集详细信息
        Map<Long, SimilarityDetails> similarityDetailsMap = new ConcurrentHashMap<>();
        Map<Long, Double> userSimilarityMap = overlapHistoryByUser.entrySet().parallelStream()
                .collect(Collectors.toConcurrentMap(
                    Map.Entry::getKey,
                    entry -> {
                        // 尝试从缓存获取
                        Long otherUserId = entry.getKey();
                        Map<Long, Double> cached = similarityCache.get(userId);
                        if (cached != null && cached.containsKey(otherUserId)) {
                            // 缓存命中时，详细信息可能不完整，但为了性能可以接受
                            return cached.get(otherUserId);
                        }
                        
                        // 计算相似度及详细信息
                        SimilarityDetails details = calculateSimilarityWithDetails(currentUserHistory, entry.getValue());
                        double similarity = 0.6 * details.getAdjustedCosine() + 0.2 * details.getJaccard() + 0.2 * details.getTimeWeighted();
                        
                        if (similarity >= MIN_SIMILARITY) {
                            similarityDetailsMap.put(otherUserId, details);
                            return similarity;
                        }
                        return null;
                    },
                    (v1, v2) -> v1 != null ? v1 : v2
                ));
        
        // 移除null值
        userSimilarityMap.entrySet().removeIf(entry -> entry.getValue() == null);
        
        // 更新缓存
        updateSimilarityCache(userId, userSimilarityMap);
        
        context.setUserSimilarityMap(userSimilarityMap);
        context.setSimilarityDetails(similarityDetailsMap);

        Map<Long, Double> candidateSpots = new HashMap<>();
        Map<Long, Double> collaborativeScores = new HashMap<>();

        if (!userSimilarityMap.isEmpty()) {
            List<Long> similarUserIds = userSimilarityMap.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(MAX_SIMILAR_USERS)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());

            if (!similarUserIds.isEmpty()) {
                Map<Long, List<UserVisitHistory>> similarUserHistories = historyRepository.findByUserIdIn(similarUserIds)
                        .stream()
                        .collect(Collectors.groupingBy(history -> history.getUser().getId()));

                for (Long similarUserId : similarUserIds) {
                    double similarity = userSimilarityMap.get(similarUserId);
                    List<UserVisitHistory> histories = similarUserHistories.getOrDefault(similarUserId, Collections.emptyList());
                    
                    // 计算相似用户的平均评分（用于偏差调整）
                    double similarUserAvg = calculateUserAverageRating(histories);
                    double currentUserAvg = calculateUserAverageRating(currentUserHistory);
                    
                    for (UserVisitHistory history : histories) {
                        Long spotId = history.getSpot().getId();
                        if (spotId == null || visitedSpotIds.contains(spotId)) {
                            continue;
                        }
                        
                        // 调整后的评分（考虑用户评分偏差）
                        double rawRating = normalizeRating(history.getRating());
                        double adjustedRating = rawRating - similarUserAvg + currentUserAvg;
                        adjustedRating = Math.max(1.0, Math.min(5.0, adjustedRating)); // 限制在1-5范围内
                        
                        double recencyBoost = calculateRecencyBoost(history.getVisitDate());
                        double engagementWeight = calculateEngagementWeight(history.getClickCount(), history.getDwellSeconds());
                        
                        // 增强的评分计算
                        double score = similarity * (adjustedRating + recencyBoost + engagementWeight);
                        collaborativeScores.merge(spotId, score, this::accumulateScores);
                        candidateSpots.merge(spotId, score, this::accumulateScores);
                    }
                }
            }
        }

        // 内容过滤得分（标签匹配）
        Map<Long, Double> tagBasedScores = scoreSpotsByTags(tagPreferenceProfile, visitedSpotIds);
        Map<Long, Double> weightedTagScores = new HashMap<>();
        tagBasedScores.forEach((spotId, score) -> {
            double weighted = score * TAG_SCORE_MULTIPLIER * CONTENT_WEIGHT;
            weightedTagScores.put(spotId, weighted);
            candidateSpots.merge(spotId, weighted, this::accumulateScores);
        });

        // 归一化协同过滤得分
        Map<Long, Double> normalizedCollaborativeScores = normalizeScores(collaborativeScores);
        normalizedCollaborativeScores.forEach((spotId, score) -> {
            candidateSpots.merge(spotId, score * COLLABORATIVE_WEIGHT, this::accumulateScores);
        });

        context.setCollaborativeScores(normalizedCollaborativeScores);
        context.setTagScores(weightedTagScores);
        context.setCandidateScores(candidateSpots);

        // 应用多样性惩罚和探索机制
        List<ScenicSpot> recommendations = rerankWithDiversityAndExploration(
                candidateSpots, visitedSpotIds, tagPreferenceProfile);

        context.setRecommendations(recommendations);

        if (recommendations.isEmpty()) {
            context.setFallbackUsed(true);
            recommendations = fallbackPopularSpots();
        }

        context.setRecommendations(recommendations);
        return context;
    }

    private List<HistoryEntry> buildHistoryEntries(List<UserVisitHistory> histories) {
        if (histories == null) {
            return Collections.emptyList();
        }
        return histories.stream()
                .filter(history -> history.getSpot() != null)
                .map(history -> {
                    HistoryEntry entry = new HistoryEntry();
                    entry.setSpotId(history.getSpot().getId());
                    entry.setSpotName(history.getSpot().getName());
                    entry.setRating(history.getRating() == null ? null : history.getRating().doubleValue());
                    entry.setVisitDate(history.getVisitDate());
                    return entry;
                })
                .collect(Collectors.toList());
    }

    private List<SimilarUserEntry> buildSimilarUserEntries(RecommendationComputationContext context, Map<Long, Double> userSimilarityMap) {
        if (userSimilarityMap == null || userSimilarityMap.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 从上下文中获取详细的相似度信息
        Map<Long, SimilarityDetails> similarityDetails = context.getSimilarityDetails();
        
        return userSimilarityMap.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(entry -> {
                    SimilarUserEntry similarUserEntry = new SimilarUserEntry();
                    similarUserEntry.setUserId(entry.getKey());
                    similarUserEntry.setSimilarity(entry.getValue());
                    
                    // 添加详细的相似度分解信息
                    SimilarityDetails details = similarityDetails != null ? similarityDetails.get(entry.getKey()) : null;
                    if (details != null) {
                        similarUserEntry.setAdjustedCosine(details.getAdjustedCosine());
                        similarUserEntry.setJaccard(details.getJaccard());
                        similarUserEntry.setTimeWeighted(details.getTimeWeighted());
                        similarUserEntry.setCommonSpotsCount(details.getCommonSpotsCount());
                    }
                    
                    return similarUserEntry;
                })
                .collect(Collectors.toList());
    }

    private List<CandidateScoreEntry> buildCandidateEntries(RecommendationComputationContext context) {
        Map<Long, Double> finalScores = context.getCandidateScores();
        if (finalScores == null || finalScores.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, Double> collaborativeScores = context.getCollaborativeScores();
        Map<Long, Double> tagScores = context.getTagScores();

        List<Long> candidateIds = new ArrayList<>(finalScores.keySet());
        Map<Long, String> spotNames = spotRepository.findAllById(candidateIds).stream()
                .collect(Collectors.toMap(ScenicSpot::getId, ScenicSpot::getName));

        return finalScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(entry -> {
                    CandidateScoreEntry candidateEntry = new CandidateScoreEntry();
                    Long spotId = entry.getKey();
                    candidateEntry.setSpotId(spotId);
                    candidateEntry.setSpotName(spotNames.getOrDefault(spotId, "未知景点"));
                    candidateEntry.setFinalScore(entry.getValue());
                    candidateEntry.setCollaborativeScore(collaborativeScores.getOrDefault(spotId, 0.0));
                    candidateEntry.setTagScore(tagScores.getOrDefault(spotId, 0.0));
                    return candidateEntry;
                })
                .collect(Collectors.toList());
    }

    private static class RecommendationComputationContext {
        private boolean hasHistory;
        private boolean fallbackUsed;
        private List<UserVisitHistory> currentUserHistory = Collections.emptyList();
        private Map<Long, Double> userSimilarityMap = Collections.emptyMap();
        private Map<Long, SimilarityDetails> similarityDetails = Collections.emptyMap();
        private Map<String, Double> tagProfile = Collections.emptyMap();
        private Map<Long, Double> collaborativeScores = Collections.emptyMap();
        private Map<Long, Double> tagScores = Collections.emptyMap();
        private Map<Long, Double> candidateScores = Collections.emptyMap();
        private List<ScenicSpot> recommendations = Collections.emptyList();

        public boolean hasHistory() {
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

        public List<UserVisitHistory> getCurrentUserHistory() {
            return currentUserHistory;
        }

        public void setCurrentUserHistory(List<UserVisitHistory> currentUserHistory) {
            this.currentUserHistory = currentUserHistory;
        }

        public Map<Long, Double> getUserSimilarityMap() {
            return userSimilarityMap;
        }

        public void setUserSimilarityMap(Map<Long, Double> userSimilarityMap) {
            this.userSimilarityMap = userSimilarityMap;
        }

        public Map<Long, SimilarityDetails> getSimilarityDetails() {
            return similarityDetails;
        }

        public void setSimilarityDetails(Map<Long, SimilarityDetails> similarityDetails) {
            this.similarityDetails = similarityDetails;
        }

        public Map<String, Double> getTagProfile() {
            return tagProfile;
        }

        public void setTagProfile(Map<String, Double> tagProfile) {
            this.tagProfile = tagProfile;
        }

        public Map<Long, Double> getCollaborativeScores() {
            return collaborativeScores;
        }

        public void setCollaborativeScores(Map<Long, Double> collaborativeScores) {
            this.collaborativeScores = collaborativeScores;
        }

        public Map<Long, Double> getTagScores() {
            return tagScores;
        }

        public void setTagScores(Map<Long, Double> tagScores) {
            this.tagScores = tagScores;
        }

        public Map<Long, Double> getCandidateScores() {
            return candidateScores;
        }

        public void setCandidateScores(Map<Long, Double> candidateScores) {
            this.candidateScores = candidateScores;
        }

        public List<ScenicSpot> getRecommendations() {
            return recommendations;
        }

        public void setRecommendations(List<ScenicSpot> recommendations) {
            this.recommendations = recommendations;
        }
    }

    /**
     * 归一化得分 - 将得分映射到 [0, 1] 范围
     */
    private Map<Long, Double> normalizeScores(Map<Long, Double> scores) {
        if (scores.isEmpty()) return Collections.emptyMap();
        
        double maxScore = scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        double minScore = scores.values().stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double range = maxScore - minScore;
        
        if (range == 0) {
            // 如果所有得分相同，返回原始值
            return new HashMap<>(scores);
        }
        
        Map<Long, Double> normalized = new HashMap<>();
        scores.forEach((spotId, score) -> {
            double normalizedScore = (score - minScore) / range;
            normalized.put(spotId, normalizedScore);
        });
        
        return normalized;
    }
    
    /**
     * 重排序 - 应用多样性惩罚和探索机制
     */
    private List<ScenicSpot> rerankWithDiversityAndExploration(
            Map<Long, Double> candidateScores,
            Set<Long> visitedSpotIds,
            Map<String, Double> tagProfile) {
        
        if (candidateScores.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 获取所有候选景点
        List<Long> candidateIds = new ArrayList<>(candidateScores.keySet());
        Map<Long, ScenicSpot> spotMap = spotRepository.findAllById(candidateIds).stream()
                .collect(Collectors.toMap(ScenicSpot::getId, spot -> spot));
        
        // 计算多样性惩罚后的得分
        List<Long> selectedSpots = new ArrayList<>();
        
        Random random = new Random();
        int explorationCount = (int) (MAX_RESULTS * EXPLORATION_RATE);
        
        for (int i = 0; i < MAX_RESULTS && !candidateIds.isEmpty(); i++) {
            // ε-greedy: 探索机制
            if (i < explorationCount && random.nextDouble() < EXPLORATION_RATE) {
                // 随机选择一个低曝光景点
                Long randomSpotId = candidateIds.get(random.nextInt(candidateIds.size()));
                ScenicSpot spot = spotMap.get(randomSpotId);
                if (spot != null) {
                    selectedSpots.add(randomSpotId);
                    candidateIds.remove(randomSpotId);
                    continue;
                }
            }
            
            // 计算每个候选的多样性调整得分
            Map<Long, Double> adjustedScores = new HashMap<>();
            for (Long spotId : candidateIds) {
                double baseScore = candidateScores.getOrDefault(spotId, 0.0);
                double diversityPenalty = calculateDiversityPenalty(spotId, selectedSpots, spotMap);
                adjustedScores.put(spotId, baseScore * (1.0 - diversityPenalty));
            }
            
            // 选择得分最高的
            Long bestSpotId = adjustedScores.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
            
            if (bestSpotId != null) {
                selectedSpots.add(bestSpotId);
                candidateIds.remove(bestSpotId);
            }
        }
        
        // 转换为景点列表
        return selectedSpots.stream()
                .map(spotMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
    
    /**
     * 计算多样性惩罚 - 避免推荐过于相似的景点
     */
    private double calculateDiversityPenalty(Long spotId, List<Long> selectedSpots, Map<Long, ScenicSpot> spotMap) {
        if (selectedSpots.isEmpty()) return 0.0;
        
        ScenicSpot currentSpot = spotMap.get(spotId);
        if (currentSpot == null) return 0.0;
        
        double maxSimilarity = 0.0;
        for (Long selectedId : selectedSpots) {
            ScenicSpot selectedSpot = spotMap.get(selectedId);
            if (selectedSpot == null) continue;
            
            // 基于标签的相似度
            double tagSimilarity = calculateTagSimilarity(currentSpot, selectedSpot);
            // 基于类别的相似度
            double categorySimilarity = currentSpot.getCategory() == selectedSpot.getCategory() ? 1.0 : 0.0;
            
            double totalSimilarity = 0.7 * tagSimilarity + 0.3 * categorySimilarity;
            maxSimilarity = Math.max(maxSimilarity, totalSimilarity);
        }
        
        return maxSimilarity * DIVERSITY_PENALTY;
    }
    
    /**
     * 计算标签相似度
     */
    private double calculateTagSimilarity(ScenicSpot spot1, ScenicSpot spot2) {
        List<String> tags1 = extractTagValues(spot1.getTags());
        List<String> tags2 = extractTagValues(spot2.getTags());
        
        if (tags1.isEmpty() || tags2.isEmpty()) return 0.0;
        
        Set<String> set1 = new HashSet<>(tags1);
        Set<String> set2 = new HashSet<>(tags2);
        
        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        
        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    /**
     * 更新相似度缓存
     */
    private void updateSimilarityCache(Long userId, Map<Long, Double> similarities) {
        if (similarityCache.size() >= CACHE_SIZE_LIMIT) {
            // 简单的LRU策略：清除最旧的缓存（这里简化处理，实际可以使用LinkedHashMap实现真正的LRU）
            if (similarityCache.size() >= CACHE_SIZE_LIMIT * 1.5) {
                similarityCache.clear();
            }
        }
        similarityCache.put(userId, new HashMap<>(similarities));
    }
    
    /**
     * 清除用户缓存（当用户行为更新时调用）
     */
    public void invalidateUserCache(Long userId) {
        similarityCache.remove(userId);
        tagProfileCache.remove(userId);
    }

    private Double accumulateScores(Double existing, Double addition) {
        double left = existing == null ? 0.0d : existing;
        double right = addition == null ? 0.0d : addition;
        return left + right;
    }
}
