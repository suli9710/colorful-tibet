package com.tibet.tourism.service;

import com.tibet.tourism.dto.RecommendationContext;
import com.tibet.tourism.dto.RecommendationDebugResponse;
import com.tibet.tourism.dto.RecommendationDebugResponse.CandidateScoreEntry;
import com.tibet.tourism.dto.RecommendationDebugResponse.HistoryEntry;
import com.tibet.tourism.dto.RecommendationDebugResponse.SimilarUserEntry;
import com.tibet.tourism.entity.ScenicSpot;
import com.tibet.tourism.entity.UserVisitHistory;
import com.tibet.tourism.repository.ScenicSpotRepository;
import com.tibet.tourism.repository.UserVisitHistoryRepository;
import com.tibet.tourism.service.recommendation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationService.class);

    private final UserVisitHistoryRepository historyRepository;
    private final ScenicSpotRepository spotRepository;
    private final CompanionInferenceService companionInferenceService;
    private final ItemBasedRecommendationService itemBasedRecommendationService;
    private final ColdStartOptimizationService coldStartOptimizationService;
    private final UserBasedCFStrategy userBasedCFStrategy;
    private final ContentBasedStrategy contentBasedStrategy;
    private final ContextAwarePostProcessor contextAwarePostProcessor;
    private final DiversityReranker diversityReranker;
    private final RecommendationCacheService cacheService;
    private final RecommendationConfig config;

    public RecommendationService(UserVisitHistoryRepository historyRepository,
                                 ScenicSpotRepository spotRepository,
                                 CompanionInferenceService companionInferenceService,
                                 ItemBasedRecommendationService itemBasedRecommendationService,
                                 ColdStartOptimizationService coldStartOptimizationService,
                                 UserBasedCFStrategy userBasedCFStrategy,
                                 ContentBasedStrategy contentBasedStrategy,
                                 ContextAwarePostProcessor contextAwarePostProcessor,
                                 DiversityReranker diversityReranker,
                                 RecommendationCacheService cacheService,
                                 RecommendationConfig config) {
        this.historyRepository = historyRepository;
        this.spotRepository = spotRepository;
        this.companionInferenceService = companionInferenceService;
        this.itemBasedRecommendationService = itemBasedRecommendationService;
        this.coldStartOptimizationService = coldStartOptimizationService;
        this.userBasedCFStrategy = userBasedCFStrategy;
        this.contentBasedStrategy = contentBasedStrategy;
        this.contextAwarePostProcessor = contextAwarePostProcessor;
        this.diversityReranker = diversityReranker;
        this.cacheService = cacheService;
        this.config = config;
    }

    public List<ScenicSpot> recommendSpotsForUser(Long userId) {
        return recommendSpotsForUser(userId, null);
    }

    public List<ScenicSpot> recommendSpotsForUser(Long userId, RecommendationContext recommendationContext) {
        logger.info("🎯 开始为用户 {} 生成推荐", userId);
        ComputationContext ctx = computeContext(userId, recommendationContext);
        logger.info("✅ 推荐完成，共生成 {} 个推荐结果", ctx.recommendations.size());
        return ctx.recommendations;
    }

    public RecommendationDebugResponse recommendWithDebug(Long userId) {
        return recommendWithDebug(userId, null);
    }

    public RecommendationDebugResponse recommendWithDebug(Long userId, RecommendationContext recommendationContext) {
        long startTime = System.currentTimeMillis();
        logger.info("🎯 [DEBUG模式] 开始为用户 {} 生成推荐", userId);

        ComputationContext ctx = computeContext(userId, recommendationContext);

        RecommendationDebugResponse response = new RecommendationDebugResponse();
        response.setUserId(userId);
        response.setHasHistory(ctx.hasHistory);
        response.setFallbackUsed(ctx.fallbackUsed);
        response.setRecommendations(ctx.recommendations);
        response.setTagProfile(ctx.tagProfile);
        response.setHistory(buildHistoryEntries(ctx.currentUserHistory));
        response.setSimilarUsers(Collections.emptyList());
        response.setCandidateScores(buildCandidateEntries(ctx));

        Map<String, Object> algorithmConfig = new HashMap<>();
        algorithmConfig.put("maxSimilarUsers", config.getMaxSimilarUsers());
        algorithmConfig.put("maxResults", config.getMaxResults());
        algorithmConfig.put("minSimilarity", config.getMinSimilarity());
        algorithmConfig.put("collaborativeWeight", config.getCollaborativeWeight());
        algorithmConfig.put("contentWeight", config.getContentWeight());
        algorithmConfig.put("diversityPenalty", config.getDiversityPenalty());
        algorithmConfig.put("explorationRate", config.getExplorationRate());
        response.setAlgorithmConfig(algorithmConfig);

        response.setRecommendationReasons(generateRecommendationReasons(ctx));
        response.setComputationTimeMs(System.currentTimeMillis() - startTime);

        logger.info("✅ [DEBUG模式] 推荐完成，耗时: {}ms", response.getComputationTimeMs());
        return response;
    }

    public void invalidateUserCache(Long userId) {
        cacheService.invalidateUserCache(userId);
    }

    // --- pipeline orchestration ---

    private ComputationContext computeContext(Long userId, RecommendationContext recommendationContext) {
        ComputationContext ctx = new ComputationContext();

        if (recommendationContext == null || recommendationContext.getCompanion() == null) {
            try {
                String inferredCompanion = companionInferenceService.getCompanionType(userId);
                if (recommendationContext == null) recommendationContext = new RecommendationContext();
                recommendationContext.setCompanion(inferredCompanion);
            } catch (Exception e) {
                logger.warn("⚠️  旅伴类型推断失败: {}", e.getMessage());
            }
        }

        List<UserVisitHistory> currentUserHistory = historyRepository.findByUserId(userId);
        ctx.currentUserHistory = currentUserHistory;
        ctx.hasHistory = !currentUserHistory.isEmpty();

        if (currentUserHistory.isEmpty()) {
            ctx.fallbackUsed = true;
            List<ScenicSpot> coldStartResults;
            if (recommendationContext != null) {
                coldStartResults = coldStartOptimizationService.hybridColdStartRecommendation(
                        userId,
                        recommendationContext.getCurrentLatitude() != null ? recommendationContext.getCurrentLatitude().doubleValue() : null,
                        recommendationContext.getCurrentLongitude() != null ? recommendationContext.getCurrentLongitude().doubleValue() : null,
                        recommendationContext.getPreferredActivities() != null ? Arrays.asList(recommendationContext.getPreferredActivities().split(",")) : null,
                        recommendationContext.getSeason(),
                        recommendationContext.getCompanion());
            } else {
                coldStartResults = coldStartOptimizationService.recommendForNewUserByAttributes(userId);
            }
            ctx.recommendations = coldStartResults;
            return ctx;
        }

        Set<Long> visitedSpotIds = currentUserHistory.stream()
                .map(h -> h.getSpot().getId())
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (visitedSpotIds.isEmpty()) {
            ctx.fallbackUsed = true;
            ctx.recommendations = fallbackPopularSpots();
            return ctx;
        }

        // Tag profile (also cached inside ContentBasedStrategy)
        ctx.tagProfile = contentBasedStrategy.getTagProfile(userId, currentUserHistory, visitedSpotIds);

        // User-Based CF scores
        Map<Long, Double> userBasedScores = userBasedCFStrategy.score(userId, currentUserHistory, visitedSpotIds);
        Map<Long, Double> normalizedUserBased = normalizeScores(userBasedScores);

        // Item-Based CF scores
        Map<Long, Double> itemBasedScores = Collections.emptyMap();
        try {
            itemBasedScores = itemBasedRecommendationService.recommendByItemCF(userId, visitedSpotIds);
            logger.info("🎯 Item-Based CF生成 {} 个候选景点", itemBasedScores.size());
        } catch (Exception e) {
            logger.warn("⚠️  Item-Based CF推荐失败: {}", e.getMessage());
        }
        Map<Long, Double> normalizedItemBased = normalizeScores(itemBasedScores);

        // Blend user-based + item-based
        Map<Long, Double> hybridCollaborative = new HashMap<>();
        normalizedUserBased.forEach((spotId, score) -> hybridCollaborative.put(spotId, score * config.getUserBasedWeight()));
        normalizedItemBased.forEach((spotId, score) -> hybridCollaborative.merge(spotId, score * config.getItemBasedWeight(), Double::sum));

        // Candidate accumulation
        Map<Long, Double> candidateSpots = new HashMap<>();
        hybridCollaborative.forEach((spotId, score) -> candidateSpots.merge(spotId, score * config.getCollaborativeWeight(), Double::sum));

        // Content-based scores
        Map<Long, Double> tagScores = contentBasedStrategy.score(userId, currentUserHistory, visitedSpotIds);
        Map<Long, Double> weightedTagScores = new HashMap<>();
        tagScores.forEach((spotId, score) -> {
            double weighted = score * config.getTagScoreMultiplier() * config.getContentWeight();
            weightedTagScores.put(spotId, weighted);
            candidateSpots.merge(spotId, weighted, Double::sum);
        });

        ctx.collaborativeScores = hybridCollaborative;
        ctx.userBasedScores = normalizedUserBased;
        ctx.itemBasedScores = normalizedItemBased;
        ctx.tagScores = weightedTagScores;
        ctx.candidateScores = candidateSpots;

        // Context-aware post-processing
        Map<Long, Double> finalCandidates = candidateSpots;
        Map<Long, ScenicSpot> spotMap = null;
        if (recommendationContext != null) {
            List<Long> candidateIds = new ArrayList<>(candidateSpots.keySet());
            spotMap = spotRepository.findAllById(candidateIds).stream()
                    .collect(Collectors.toMap(ScenicSpot::getId, s -> s));
            finalCandidates = contextAwarePostProcessor.apply(candidateSpots, recommendationContext, spotMap);
        }

        // Diversity reranking
        List<ScenicSpot> recommendations = diversityReranker.rerank(finalCandidates, visitedSpotIds, spotMap);

        if (recommendations.isEmpty()) {
            ctx.fallbackUsed = true;
            recommendations = fallbackPopularSpots();
        }

        ctx.recommendations = recommendations;
        return ctx;
    }

    // --- helpers ---

    private List<ScenicSpot> fallbackPopularSpots() {
        return spotRepository.findAllWithoutTags(PageRequest.of(
                        0,
                        Math.max(1, config.getMaxResults()),
                        Sort.by(Sort.Direction.DESC, "visitCount").and(Sort.by("id"))))
                .getContent();
    }

    private Map<Long, Double> normalizeScores(Map<Long, Double> scores) {
        if (scores.isEmpty()) return Collections.emptyMap();
        double max = scores.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        double min = scores.values().stream().mapToDouble(Double::doubleValue).min().orElse(0.0);
        double range = max - min;
        if (range == 0) return new HashMap<>(scores);
        Map<Long, Double> normalized = new HashMap<>();
        scores.forEach((id, score) -> normalized.put(id, (score - min) / range));
        return normalized;
    }

    private List<HistoryEntry> buildHistoryEntries(List<UserVisitHistory> histories) {
        if (histories == null) return Collections.emptyList();
        return histories.stream()
                .filter(h -> h.getSpot() != null)
                .map(h -> {
                    HistoryEntry entry = new HistoryEntry();
                    entry.setSpotId(h.getSpot().getId());
                    entry.setSpotName(h.getSpot().getName());
                    entry.setRating(h.getRating() == null ? null : h.getRating().doubleValue());
                    entry.setVisitDate(h.getVisitDate());
                    return entry;
                })
                .collect(Collectors.toList());
    }

    private List<CandidateScoreEntry> buildCandidateEntries(ComputationContext ctx) {
        Map<Long, Double> finalScores = ctx.candidateScores;
        if (finalScores == null || finalScores.isEmpty()) return Collections.emptyList();

        List<Long> candidateIds = new ArrayList<>(finalScores.keySet());
        Map<Long, String> spotNames = spotRepository.findAllById(candidateIds).stream()
                .collect(Collectors.toMap(ScenicSpot::getId, ScenicSpot::getName));

        return finalScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .map(entry -> {
                    CandidateScoreEntry e = new CandidateScoreEntry();
                    Long spotId = entry.getKey();
                    e.setSpotId(spotId);
                    e.setSpotName(spotNames.getOrDefault(spotId, "未知景点"));
                    e.setFinalScore(entry.getValue());
                    e.setCollaborativeScore(ctx.collaborativeScores.getOrDefault(spotId, 0.0));
                    e.setUserBasedScore(ctx.userBasedScores.getOrDefault(spotId, 0.0));
                    e.setItemBasedScore(ctx.itemBasedScores.getOrDefault(spotId, 0.0));
                    e.setTagScore(ctx.tagScores.getOrDefault(spotId, 0.0));
                    return e;
                })
                .collect(Collectors.toList());
    }

    private Map<Long, String> generateRecommendationReasons(ComputationContext ctx) {
        Map<Long, String> reasons = new HashMap<>();
        if (ctx.recommendations == null || ctx.recommendations.isEmpty()) return reasons;

        for (ScenicSpot spot : ctx.recommendations) {
            Long spotId = spot.getId();
            if (spotId == null) continue;

            List<String> parts = new ArrayList<>();
            if (ctx.fallbackUsed) {
                if (spot.getVisitCount() != null && spot.getVisitCount() > 15000) parts.add("热门景点");
                if (spot.getRating() != null && spot.getRating().doubleValue() >= 4.0) parts.add("高评分");
                if (parts.isEmpty()) parts.add("值得探索");
            } else {
                double collab = ctx.collaborativeScores.getOrDefault(spotId, 0.0);
                double tag = ctx.tagScores.getOrDefault(spotId, 0.0);
                if (collab > 0.1) parts.add("相似用户也喜欢");
                if (tag > 0.05) parts.add("符合您的兴趣");
                if (spot.getVisitCount() != null && spot.getVisitCount() > 15000) parts.add("热门景点");
                if (spot.getRating() != null && spot.getRating().doubleValue() >= 4.0) parts.add("高评分");
                if (parts.isEmpty()) parts.add("为您精心挑选");
            }
            reasons.put(spotId, String.join("、", parts));
        }
        return reasons;
    }

    // --- internal state holder ---

    private static class ComputationContext {
        boolean hasHistory;
        boolean fallbackUsed;
        List<UserVisitHistory> currentUserHistory = Collections.emptyList();
        Map<String, Double> tagProfile = Collections.emptyMap();
        Map<Long, Double> collaborativeScores = Collections.emptyMap();
        Map<Long, Double> userBasedScores = Collections.emptyMap();
        Map<Long, Double> itemBasedScores = Collections.emptyMap();
        Map<Long, Double> tagScores = Collections.emptyMap();
        Map<Long, Double> candidateScores = Collections.emptyMap();
        List<ScenicSpot> recommendations = Collections.emptyList();
    }
}
