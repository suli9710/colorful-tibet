package com.tibet.tourism.service;

import com.tibet.tourism.dto.RecommendationContext;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class RecommendationService {
    
    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);

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
    
    // 上下文感知参数
    private static final double CONTEXT_WEIGHT = 0.25d; // 上下文权重（在最终得分中的占比）
    private static final double SEASONAL_MATCH_BOOST = 1.3d; // 季节性匹配增强
    private static final double WEATHER_MATCH_BOOST = 1.2d; // 天气匹配增强
    private static final double DISTANCE_BOOST_FACTOR = 0.5d; // 距离增强因子（距离越近，分数越高）
    private static final double BUDGET_PENALTY = 0.8d; // 超出预算的惩罚系数
    private static final double COMPANION_MATCH_BOOST = 1.15d; // 旅伴匹配增强

    @Autowired
    private UserVisitHistoryRepository historyRepository;

    @Autowired
    private ScenicSpotRepository spotRepository;

    @Autowired
    private SpotTagRepository spotTagRepository;
    
    @Autowired
    private CompanionInferenceService companionInferenceService;
    
    @Autowired
    private ItemBasedRecommendationService itemBasedRecommendationService;
    
    @Autowired
    private ColdStartOptimizationService coldStartOptimizationService;
    
    // 缓存：用户相似度映射（可扩展为Redis缓存）
    private final Map<Long, Map<Long, Double>> similarityCache = new ConcurrentHashMap<>();
    private final Map<Long, Map<String, Double>> tagProfileCache = new ConcurrentHashMap<>();
    private static final int CACHE_SIZE_LIMIT = 1000; // 缓存大小限制
    
    // 混合推荐权重配置
    private static final double USER_BASED_WEIGHT = 0.3d; // User-Based CF权重
    private static final double ITEM_BASED_WEIGHT = 0.7d; // Item-Based CF权重

    public List<ScenicSpot> recommendSpotsForUser(Long userId) {
        return recommendSpotsForUser(userId, null);
    }
    
    public List<ScenicSpot> recommendSpotsForUser(Long userId, RecommendationContext recommendationContext) {
        logger.info("═══════════════════════════════════════════════════════════");
        logger.info("🎯 开始为用户 {} 生成推荐", userId);
        if (recommendationContext != null) {
            logger.info("📌 上下文信息: 季节={}, 天气={}, 位置={}, 预算={}", 
                    recommendationContext.getSeason(), 
                    recommendationContext.getWeather(),
                    recommendationContext.getCurrentLocation(),
                    recommendationContext.getBudget());
        }
        logger.info("═══════════════════════════════════════════════════════════");
        
        RecommendationComputationContext context = computeContext(userId, recommendationContext);
        
        logger.info("✅ 推荐完成，共生成 {} 个推荐结果", context.getRecommendations().size());
        logger.info("═══════════════════════════════════════════════════════════\n");
        
        return context.getRecommendations();
    }

    public RecommendationDebugResponse recommendWithDebug(Long userId) {
        return recommendWithDebug(userId, null);
    }
    
    public RecommendationDebugResponse recommendWithDebug(Long userId, RecommendationContext recommendationContext) {
        long startTime = System.currentTimeMillis();
        
        logger.info("═══════════════════════════════════════════════════════════");
        logger.info("🎯 [DEBUG模式] 开始为用户 {} 生成推荐", userId);
        if (recommendationContext != null) {
            logger.info("📌 上下文信息: 季节={}, 天气={}, 位置={}, 预算={}", 
                    recommendationContext.getSeason(), 
                    recommendationContext.getWeather(),
                    recommendationContext.getCurrentLocation(),
                    recommendationContext.getBudget());
        }
        logger.info("═══════════════════════════════════════════════════════════");
        
        RecommendationComputationContext context = computeContext(userId, recommendationContext);
        
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
        
        logger.info("✅ [DEBUG模式] 推荐完成，耗时: {}ms", endTime - startTime);
        logger.info("═══════════════════════════════════════════════════════════\n");
        
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
        return computeContext(userId, null);
    }
    
    private RecommendationComputationContext computeContext(Long userId, RecommendationContext recommendationContext) {
        RecommendationComputationContext context = new RecommendationComputationContext();
        
        // 如果上下文未提供旅伴类型，尝试自动推断
        if (recommendationContext == null || recommendationContext.getCompanion() == null) {
            try {
                String inferredCompanion = companionInferenceService.getCompanionType(userId);
                if (recommendationContext == null) {
                    recommendationContext = new RecommendationContext();
                }
                recommendationContext.setCompanion(inferredCompanion);
                logger.info("🔍 自动推断旅伴类型: {}", inferredCompanion);
            } catch (Exception e) {
                logger.warn("⚠️  旅伴类型推断失败: {}", e.getMessage());
            }
        }
        
        context.setRecommendationContext(recommendationContext);

        List<UserVisitHistory> currentUserHistory = historyRepository.findByUserId(userId);
        context.setCurrentUserHistory(currentUserHistory);
        context.setHasHistory(!currentUserHistory.isEmpty());

        logger.info("📊 用户历史记录: {} 条访问记录", currentUserHistory.size());

        if (currentUserHistory.isEmpty()) {
            logger.warn("⚠️  用户无历史记录，使用冷启动优化策略");
            context.setFallbackUsed(true);
            
            // 使用冷启动优化服务
            List<ScenicSpot> coldStartRecommendations;
            if (recommendationContext != null) {
                // 如果有上下文信息（位置、偏好等），使用混合冷启动推荐
                coldStartRecommendations = coldStartOptimizationService.hybridColdStartRecommendation(
                        userId,
                        recommendationContext.getCurrentLatitude() != null ? 
                                recommendationContext.getCurrentLatitude().doubleValue() : null,
                        recommendationContext.getCurrentLongitude() != null ? 
                                recommendationContext.getCurrentLongitude().doubleValue() : null,
                        recommendationContext.getPreferredActivities() != null ? 
                                Arrays.asList(recommendationContext.getPreferredActivities().split(",")) : null,
                        recommendationContext.getSeason(), // 可以作为类别参考
                        recommendationContext.getCompanion()
                );
            } else {
                // 否则使用基于用户属性的推荐
                coldStartRecommendations = coldStartOptimizationService.recommendForNewUserByAttributes(userId);
            }
            
            context.setRecommendations(coldStartRecommendations);
            logger.info("📌 冷启动推荐返回 {} 个景点", context.getRecommendations().size());
            return context;
        }
        
        // 检查是否为新用户（访问记录少于3条）
        if (coldStartOptimizationService.isNewUser(userId)) {
            logger.info("🆕 检测到新用户（访问记录<3），增强冷启动推荐");
            
            // 对于新用户，可以混合使用冷启动推荐和少量协同过滤
            // 这里先使用冷启动推荐，后续可以优化为混合策略
        }

        Set<Long> visitedSpotIds = currentUserHistory.stream()
                .map(history -> history.getSpot().getId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (visitedSpotIds.isEmpty()) {
            logger.warn("⚠️  访问记录中无有效景点ID，使用热门景点兜底策略");
            context.setFallbackUsed(true);
            context.setRecommendations(fallbackPopularSpots());
            return context;
        }

        logger.info("📍 已访问景点数: {}", visitedSpotIds.size());

        // 尝试从缓存获取标签画像
        Map<String, Double> tagPreferenceProfile = tagProfileCache.get(userId);
        if (tagPreferenceProfile == null) {
            tagPreferenceProfile = buildUserTagProfile(currentUserHistory, visitedSpotIds);
            // 更新缓存
            if (tagProfileCache.size() < CACHE_SIZE_LIMIT) {
                tagProfileCache.put(userId, tagPreferenceProfile);
            }
            logger.info("🏷️  构建用户标签画像: {} 个标签", tagPreferenceProfile.size());
        } else {
            logger.info("🏷️  从缓存获取标签画像: {} 个标签", tagPreferenceProfile.size());
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
        
        logger.info("👥 找到 {} 个相似用户（相似度 >= {}）", userSimilarityMap.size(), MIN_SIMILARITY);
        if (!userSimilarityMap.isEmpty()) {
            userSimilarityMap.entrySet().stream()
                    .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                    .limit(5)
                    .forEach(entry -> {
                        SimilarityDetails details = similarityDetailsMap.get(entry.getKey());
                        if (details != null) {
                            logger.info("   - 用户 {}: 总相似度={:.4f} (余弦={:.4f}, Jaccard={:.4f}, 时间={:.4f}, 共同景点={})",
                                    entry.getKey(), entry.getValue(), 
                                    details.getAdjustedCosine(), details.getJaccard(), 
                                    details.getTimeWeighted(), details.getCommonSpotsCount());
                        } else {
                            logger.info("   - 用户 {}: 相似度={:.4f}", entry.getKey(), entry.getValue());
                        }
                    });
        }
        
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

            logger.info("🔄 使用前 {} 个相似用户生成候选推荐", Math.min(similarUserIds.size(), MAX_SIMILAR_USERS));

            if (!similarUserIds.isEmpty()) {
                Map<Long, List<UserVisitHistory>> similarUserHistories = historyRepository.findByUserIdIn(similarUserIds)
                        .stream()
                        .collect(Collectors.groupingBy(history -> history.getUser().getId()));

                int candidateCount = 0;
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
                        candidateCount++;
                    }
                }
                logger.info("📊 协同过滤生成 {} 个候选景点", candidateCount);
            }
        } else {
            logger.warn("⚠️  未找到相似用户，仅使用标签匹配");
        }

        // Item-Based CF推荐得分
        Map<Long, Double> itemBasedScores = new HashMap<>();
        if (!visitedSpotIds.isEmpty()) {
            try {
                itemBasedScores = itemBasedRecommendationService.recommendByItemCF(userId, visitedSpotIds);
                logger.info("🎯 Item-Based CF生成 {} 个候选景点", itemBasedScores.size());
            } catch (Exception e) {
                logger.warn("⚠️  Item-Based CF推荐失败: {}", e.getMessage());
            }
        }
        
        // 归一化User-Based CF得分
        Map<Long, Double> normalizedCollaborativeScores = normalizeScores(collaborativeScores);
        
        // 归一化Item-Based CF得分
        Map<Long, Double> normalizedItemBasedScores = normalizeScores(itemBasedScores);
        
        // 混合User-Based和Item-Based得分
        Map<Long, Double> hybridCollaborativeScores = new HashMap<>();
        
        // 合并User-Based和Item-Based得分
        normalizedCollaborativeScores.forEach((spotId, score) -> {
            hybridCollaborativeScores.put(spotId, score * USER_BASED_WEIGHT);
        });
        
        normalizedItemBasedScores.forEach((spotId, score) -> {
            hybridCollaborativeScores.merge(spotId, score * ITEM_BASED_WEIGHT, Double::sum);
        });
        
        // 将混合得分加入候选集合
        hybridCollaborativeScores.forEach((spotId, score) -> {
            candidateSpots.merge(spotId, score * COLLABORATIVE_WEIGHT, this::accumulateScores);
        });
        
        // 内容过滤得分（标签匹配）
        Map<Long, Double> tagBasedScores = scoreSpotsByTags(tagPreferenceProfile, visitedSpotIds);
        logger.info("🏷️  标签匹配生成 {} 个候选景点", tagBasedScores.size());
        Map<Long, Double> weightedTagScores = new HashMap<>();
        tagBasedScores.forEach((spotId, score) -> {
            double weighted = score * TAG_SCORE_MULTIPLIER * CONTENT_WEIGHT;
            weightedTagScores.put(spotId, weighted);
            candidateSpots.merge(spotId, weighted, this::accumulateScores);
        });

        context.setCollaborativeScores(hybridCollaborativeScores);
        context.setTagScores(weightedTagScores);
        context.setCandidateScores(candidateSpots);
        
        // 保存详细得分用于调试
        context.setUserBasedScores(normalizedCollaborativeScores);
        context.setItemBasedScores(normalizedItemBasedScores);

        logger.info("📈 候选景点总数: {} (混合协同过滤: {}, User-Based: {}, Item-Based: {}, 标签匹配: {})", 
                candidateSpots.size(), 
                hybridCollaborativeScores.size(),
                normalizedCollaborativeScores.size(),
                normalizedItemBasedScores.size(),
                weightedTagScores.size());

        // 应用上下文感知过滤和加权
        Map<Long, Double> finalCandidateSpots = candidateSpots;
        if (recommendationContext != null) {
            finalCandidateSpots = applyContextAwareFiltering(candidateSpots, recommendationContext);
            logger.info("🌍 上下文过滤后候选景点数: {}", finalCandidateSpots.size());
        }

        // 应用多样性惩罚和探索机制（如果无上下文，传入null）
        List<ScenicSpot> recommendations = rerankWithDiversityAndExploration(
                finalCandidateSpots, visitedSpotIds, tagPreferenceProfile, recommendationContext);

        context.setRecommendations(recommendations);

        if (recommendations.isEmpty()) {
            logger.warn("⚠️  重排序后无推荐结果，使用热门景点兜底");
            context.setFallbackUsed(true);
            recommendations = fallbackPopularSpots();
        }

        logger.info("✨ 最终推荐结果: {} 个景点", recommendations.size());
        recommendations.stream()
                .limit(5)
                .forEach(spot -> logger.info("   - {}", spot.getName()));

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
        Map<Long, Double> userBasedScores = context.getUserBasedScores();
        Map<Long, Double> itemBasedScores = context.getItemBasedScores();
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
                    candidateEntry.setUserBasedScore(userBasedScores.getOrDefault(spotId, 0.0));
                    candidateEntry.setItemBasedScore(itemBasedScores.getOrDefault(spotId, 0.0));
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
        private Map<Long, Double> collaborativeScores = Collections.emptyMap(); // 混合协同过滤得分
        private Map<Long, Double> userBasedScores = Collections.emptyMap(); // User-Based CF得分
        private Map<Long, Double> itemBasedScores = Collections.emptyMap(); // Item-Based CF得分
        private Map<Long, Double> tagScores = Collections.emptyMap();
        private Map<Long, Double> candidateScores = Collections.emptyMap();
        private List<ScenicSpot> recommendations = Collections.emptyList();
        private RecommendationContext recommendationContext;

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

        public RecommendationContext getRecommendationContext() {
            return recommendationContext;
        }

        public void setRecommendationContext(RecommendationContext recommendationContext) {
            this.recommendationContext = recommendationContext;
        }

        public Map<Long, Double> getUserBasedScores() {
            return userBasedScores;
        }

        public void setUserBasedScores(Map<Long, Double> userBasedScores) {
            this.userBasedScores = userBasedScores;
        }

        public Map<Long, Double> getItemBasedScores() {
            return itemBasedScores;
        }

        public void setItemBasedScores(Map<Long, Double> itemBasedScores) {
            this.itemBasedScores = itemBasedScores;
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
     * 应用上下文感知过滤和加权
     */
    private Map<Long, Double> applyContextAwareFiltering(
            Map<Long, Double> candidateScores,
            RecommendationContext context) {
        
        if (candidateScores.isEmpty()) {
            return candidateScores;
        }
        
        // 获取所有候选景点
        List<Long> candidateIds = new ArrayList<>(candidateScores.keySet());
        Map<Long, ScenicSpot> spotMap = spotRepository.findAllById(candidateIds).stream()
                .collect(Collectors.toMap(ScenicSpot::getId, spot -> spot));
        
        Map<Long, Double> contextScores = new HashMap<>();
        Map<Long, Double> filteredScores = new HashMap<>();
        
        for (Map.Entry<Long, Double> entry : candidateScores.entrySet()) {
            Long spotId = entry.getKey();
            ScenicSpot spot = spotMap.get(spotId);
            if (spot == null) continue;
            
            // 计算上下文得分
            double contextScore = calculateContextScore(spot, context);
            
            // 如果上下文得分太低，过滤掉
            if (contextScore < 0.3) {
                continue;
            }
            
            // 应用上下文加权
            double baseScore = entry.getValue();
            double finalScore = baseScore * (1.0 - CONTEXT_WEIGHT) + contextScore * CONTEXT_WEIGHT;
            
            contextScores.put(spotId, contextScore);
            filteredScores.put(spotId, finalScore);
        }
        
        logger.info("🌍 上下文过滤: 原始{}个 -> 过滤后{}个", candidateScores.size(), filteredScores.size());
        
        return filteredScores;
    }
    
    /**
     * 计算景点的上下文得分
     */
    private double calculateContextScore(ScenicSpot spot, RecommendationContext context) {
        double score = 1.0;
        
        // 1. 季节性匹配
        if (context.getSeason() != null) {
            if (isSeasonalMatch(spot, context.getSeason())) {
                score *= SEASONAL_MATCH_BOOST;
                logger.debug("  景点 {} 季节性匹配: {}", spot.getName(), context.getSeason());
            }
        }
        
        // 2. 天气匹配
        if (context.getWeather() != null) {
            if (isWeatherSuitable(spot, context.getWeather())) {
                score *= WEATHER_MATCH_BOOST;
                logger.debug("  景点 {} 天气匹配: {}", spot.getName(), context.getWeather());
            }
        }
        
        // 3. 距离匹配
        if (context.getConsiderDistance() != null && context.getConsiderDistance() 
            && context.getCurrentLatitude() != null && context.getCurrentLongitude() != null
            && spot.getLatitude() != null && spot.getLongitude() != null) {
            double distance = calculateDistance(
                    context.getCurrentLatitude(), context.getCurrentLongitude(),
                    spot.getLatitude().doubleValue(), spot.getLongitude().doubleValue()
            );
            // 距离越近，分数越高（使用反比例函数）
            double distanceScore = 1.0 / (1.0 + distance / 100.0); // 100km为基准
            score *= (1.0 + distanceScore * DISTANCE_BOOST_FACTOR);
            logger.debug("  景点 {} 距离: {}km, 距离得分: {}", spot.getName(), distance, distanceScore);
        }
        
        // 4. 预算匹配
        if (context.getConsiderBudget() != null && context.getConsiderBudget() 
            && context.getBudget() != null && spot.getTicketPrice() != null) {
            double ticketPrice = spot.getTicketPrice().doubleValue();
            if (ticketPrice <= context.getBudget()) {
                score *= 1.1; // 在预算内，略微提升
            } else {
                score *= BUDGET_PENALTY; // 超出预算，降低分数
                logger.debug("  景点 {} 超出预算: {} > {}", spot.getName(), ticketPrice, context.getBudget());
            }
        }
        
        // 5. 旅伴匹配（基于景点特征）
        if (context.getCompanion() != null) {
            if (isCompanionSuitable(spot, context.getCompanion())) {
                score *= COMPANION_MATCH_BOOST;
                logger.debug("  景点 {} 旅伴匹配: {}", spot.getName(), context.getCompanion());
            }
        }
        
        // 6. 活动偏好匹配
        if (context.getPreferredActivities() != null && !context.getPreferredActivities().isEmpty()) {
            double activityMatch = calculateActivityMatch(spot, context.getPreferredActivities());
            score *= (1.0 + activityMatch * 0.2); // 活动匹配最多提升20%
        }
        
        return Math.min(score, 2.0); // 限制最大得分为2.0
    }
    
    /**
     * 判断景点是否适合当前季节
     */
    private boolean isSeasonalMatch(ScenicSpot spot, String season) {
        // 如果未指定季节，根据当前月份自动判断
        if (season == null) {
            int month = LocalDateTime.now().getMonthValue();
            if (month >= 3 && month <= 5) season = "SPRING";
            else if (month >= 6 && month <= 8) season = "SUMMER";
            else if (month >= 9 && month <= 11) season = "AUTUMN";
            else season = "WINTER";
        }
        
        // 西藏旅游旺季是5-10月（春夏秋），淡季是11-4月（冬春）
        // 这里简化处理：春夏秋适合大部分景点，冬季适合室内景点
        switch (season.toUpperCase()) {
            case "SPRING":
            case "SUMMER":
            case "AUTUMN":
                // 春夏秋适合大部分景点
                return true;
            case "WINTER":
                // 冬季更适合室内景点（如寺庙、博物馆）
                return spot.getCategory() == ScenicSpot.Category.CULTURAL 
                    || spot.getCategory() == ScenicSpot.Category.RELIGIOUS;
            default:
                return true;
        }
    }
    
    /**
     * 判断景点是否适合当前天气
     */
    private boolean isWeatherSuitable(ScenicSpot spot, String weather) {
        if (weather == null) return true;
        
        switch (weather.toUpperCase()) {
            case "SUNNY":
                // 晴天适合所有景点
                return true;
            case "CLOUDY":
                // 多云适合所有景点
                return true;
            case "RAINY":
                // 雨天更适合室内景点
                return spot.getCategory() == ScenicSpot.Category.CULTURAL 
                    || spot.getCategory() == ScenicSpot.Category.RELIGIOUS;
            case "SNOWY":
                // 雪天更适合室内景点，但高海拔景点可能因雪景而加分
                return spot.getCategory() == ScenicSpot.Category.CULTURAL 
                    || spot.getCategory() == ScenicSpot.Category.RELIGIOUS
                    || (spot.getAltitude() != null && parseAltitude(spot.getAltitude()) > 4000);
            default:
                return true;
        }
    }
    
    /**
     * 判断景点是否适合旅伴类型
     */
    private boolean isCompanionSuitable(ScenicSpot spot, String companion) {
        if (companion == null) return true;
        
        switch (companion.toUpperCase()) {
            case "ALONE":
                // 独自旅行适合所有景点
                return true;
            case "COUPLE":
                // 情侣适合浪漫、风景优美的景点
                return spot.getCategory() == ScenicSpot.Category.NATURAL
                    || spot.getCategory() == ScenicSpot.Category.CULTURAL;
            case "FAMILY":
                // 家庭适合安全、易到达的景点
                return spot.getCategory() == ScenicSpot.Category.CULTURAL
                    || spot.getCategory() == ScenicSpot.Category.RELIGIOUS;
            case "FRIENDS":
            case "GROUP":
                // 朋友/团队适合所有景点
                return true;
            default:
                return true;
        }
    }
    
    /**
     * 计算活动偏好匹配度
     */
    private double calculateActivityMatch(ScenicSpot spot, String preferredActivities) {
        if (preferredActivities == null || preferredActivities.isEmpty()) {
            return 0.0;
        }
        
        String[] activities = preferredActivities.split(",");
        List<String> spotTags = extractTagValues(spot.getTags());
        
        int matchCount = 0;
        for (String activity : activities) {
            String trimmed = activity.trim().toUpperCase();
            // 检查标签中是否包含相关关键词
            for (String tag : spotTags) {
                if (tag.toUpperCase().contains(trimmed) || matchesActivityTag(trimmed, tag)) {
                    matchCount++;
                    break;
                }
            }
        }
        
        return activities.length > 0 ? (double) matchCount / activities.length : 0.0;
    }
    
    /**
     * 匹配活动标签
     */
    private boolean matchesActivityTag(String activity, String tag) {
        // 简单的关键词匹配
        Map<String, String[]> activityKeywords = new HashMap<>();
        activityKeywords.put("PHOTOGRAPHY", new String[]{"摄影", "拍照", "风景", "美景"});
        activityKeywords.put("HIKING", new String[]{"徒步", "登山", "户外"});
        activityKeywords.put("CULTURE", new String[]{"文化", "历史", "人文"});
        activityKeywords.put("RELIGION", new String[]{"宗教", "寺庙", "朝圣"});
        activityKeywords.put("NATURE", new String[]{"自然", "风光", "山水"});
        
        String[] keywords = activityKeywords.get(activity);
        if (keywords == null) return false;
        
        for (String keyword : keywords) {
            if (tag.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 计算两点之间的距离（公里）- 使用Haversine公式
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 地球半径（公里）
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
    
    /**
     * 解析海拔字符串（如"5000m" -> 5000）
     */
    private double parseAltitude(String altitude) {
        if (altitude == null || altitude.isEmpty()) {
            return 0.0;
        }
        try {
            // 移除单位，提取数字
            String numeric = altitude.replaceAll("[^0-9.]", "");
            return Double.parseDouble(numeric);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
    
    /**
     * 重排序 - 应用多样性惩罚和探索机制
     */
    private List<ScenicSpot> rerankWithDiversityAndExploration(
            Map<Long, Double> candidateScores,
            Set<Long> visitedSpotIds,
            Map<String, Double> tagProfile) {
        return rerankWithDiversityAndExploration(candidateScores, visitedSpotIds, tagProfile, null);
    }
    
    private List<ScenicSpot> rerankWithDiversityAndExploration(
            Map<Long, Double> candidateScores,
            Set<Long> visitedSpotIds,
            Map<String, Double> tagProfile,
            RecommendationContext recommendationContext) {
        
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

