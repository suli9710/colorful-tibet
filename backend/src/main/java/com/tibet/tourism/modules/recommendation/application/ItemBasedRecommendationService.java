package com.tibet.tourism.modules.recommendation.application;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.domain.UserVisitHistory;
import com.tibet.tourism.modules.user.infra.UserVisitHistoryRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 基于物品的协同过滤推荐服务
 * 计算景点之间的相似度，基于用户访问历史推荐相似景点
 */
@Service
public class ItemBasedRecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(ItemBasedRecommendationService.class);

    // 景点相似度矩阵缓存
    private volatile Map<Long, Map<Long, Double>> itemSimilarityMatrix = new ConcurrentHashMap<>();
    private volatile long itemSimilarityMatrixUpdatedAt = 0L;
    private final AtomicBoolean precomputeInProgress = new AtomicBoolean(false);
    private static final double MIN_ITEM_SIMILARITY = 0.1; // 最小景点相似度阈值
    private static final int MAX_SIMILAR_ITEMS = 50; // 每个景点最多保留的相似景点数
    private static final long SIMILARITY_MATRIX_TTL_MS = 24 * 60 * 60 * 1000L;
    private static final String REDIS_MATRIX_KEY = "recommend:item-similarity:matrix";
    private static final long REDIS_MATRIX_TTL_HOURS = 48;
    private static final TypeReference<Map<String, Object>> JSON_OBJECT = new TypeReference<>() {};

    @Value("${app.recommendation.item-similarity.max-spots:2000}")
    private int maxSpotsForPrecompute;

    @Value("${app.recommendation.item-similarity.max-histories:50000}")
    private int maxHistoriesForPrecompute;

    @Autowired
    private ScenicSpotRepository spotRepository;

    @Autowired
    private UserVisitHistoryRepository historyRepository;

    @Autowired(required = false)
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    private volatile boolean redisCacheWarningLogged = false;

    /**
     * 预计算景点相似度矩阵（离线计算）
     * 建议每天凌晨执行一次
     */
    public boolean precomputeItemSimilarityMatrix() {
        if (!precomputeInProgress.compareAndSet(false, true)) {
            logger.info("Item similarity precompute already running; skipping trigger");
            return false;
        }

        try {
            logger.info("Item similarity precompute started");

            long startTime = System.currentTimeMillis();

            List<ScenicSpot> allSpots = spotRepository.findAllWithoutTags(
                    PageRequest.of(0, Math.max(1, maxSpotsForPrecompute), Sort.by("id"))).getContent();
            Set<Long> allSpotIds = allSpots.stream()
                    .map(ScenicSpot::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            logger.info("Item similarity precompute spots loaded: spotCount={}", allSpots.size());

            if (allSpotIds.isEmpty()) {
                itemSimilarityMatrix = new ConcurrentHashMap<>();
                itemSimilarityMatrixUpdatedAt = 0L;
                logger.info("No scenic spots found, skipping item similarity precompute");
                return true;
            }

            List<UserVisitHistory> allHistories = historyRepository.findAll(
                    PageRequest.of(0, Math.max(1, maxHistoriesForPrecompute), Sort.by(Sort.Direction.DESC, "visitDate")))
                    .getContent();
            Map<Long, Map<Long, Double>> userItemMatrix = buildUserItemMatrix(allHistories);
            logger.info("Item similarity user-item matrix built: userCount={}, spotCount={}",
                    userItemMatrix.size(),
                    allSpotIds.size());

            Map<ItemPair, ItemPairStats> pairStats = buildCoVisitPairStats(userItemMatrix);
            logger.info("Item similarity co-visit pairs built: pairCount={}", pairStats.size());

            Map<Long, Map<Long, Double>> similarityMatrix = buildSimilarityMatrix(allSpotIds, pairStats);
            itemSimilarityMatrix = similarityMatrix;
            itemSimilarityMatrixUpdatedAt = System.currentTimeMillis();
            saveSimilarityMatrixToCache(similarityMatrix, itemSimilarityMatrixUpdatedAt);

            long endTime = System.currentTimeMillis();
            logger.info("Item similarity precompute completed: durationMs={}", endTime - startTime);
            logger.info("Item similarity precompute summary: averageSimilarItems={}",
                    itemSimilarityMatrix.values().stream()
                            .mapToInt(Map::size)
                            .average()
                            .orElse(0.0));
            logger.info("Item similarity matrix stored: rowCount={}", itemSimilarityMatrix.size());
            return true;
        } finally {
            precomputeInProgress.set(false);
        }
    }

    public boolean loadSimilarityMatrixFromCache() {
        String cachedValue = getRedisValue(REDIS_MATRIX_KEY, "get item similarity matrix");
        Map<Long, Map<Long, Double>> cachedMatrix = toSimilarityMatrix(cachedValue);
        if (cachedMatrix == null || cachedMatrix.isEmpty()) {
            return false;
        }

        itemSimilarityMatrix = new ConcurrentHashMap<>(cachedMatrix);
        itemSimilarityMatrixUpdatedAt = extractUpdatedAt(cachedValue).orElse(System.currentTimeMillis());
        logger.info("Item similarity matrix loaded from Redis: rows={}, updatedAt={}",
                cachedMatrix.size(), itemSimilarityMatrixUpdatedAt);
        return true;
    }

    public Map<String, Object> getSimilarityMatrixStatus() {
        Map<Long, Map<Long, Double>> snapshot = itemSimilarityMatrix;
        int rowCount = snapshot.size();
        int nonEmptyRows = (int) snapshot.values().stream().filter(row -> row != null && !row.isEmpty()).count();
        int relationCount = snapshot.values().stream()
                .filter(Objects::nonNull)
                .mapToInt(Map::size)
                .sum();

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("loaded", !snapshot.isEmpty());
        status.put("precomputeInProgress", precomputeInProgress.get());
        status.put("stale", isSimilarityMatrixStale());
        status.put("rowCount", rowCount);
        status.put("nonEmptyRows", nonEmptyRows);
        status.put("relationCount", relationCount);
        status.put("updatedAtEpochMs", itemSimilarityMatrixUpdatedAt == 0L ? null : itemSimilarityMatrixUpdatedAt);
        status.put("ageMs", itemSimilarityMatrixUpdatedAt == 0L ? null : System.currentTimeMillis() - itemSimilarityMatrixUpdatedAt);
        return status;
    }

    /**
     * 构建用户-景点评分矩阵
     * 同一用户对同一景点的多条历史取最高分（保留最强的兴趣信号）
     */
    private Map<Long, Map<Long, Double>> buildUserItemMatrix(List<UserVisitHistory> histories) {
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();

        for (UserVisitHistory history : histories) {
            if (history.getUser() == null || history.getSpot() == null) {
                continue;
            }

            Long userId = history.getUser().getId();
            Long spotId = history.getSpot().getId();
            if (userId == null || spotId == null) {
                continue;
            }

            double rating = normalizeRating(history.getRating());
            double timeWeight = calculateTimeWeight(history.getVisitDate());
            double engagementWeight = calculateEngagementWeight(
                    history.getClickCount(),
                    history.getDwellSeconds()
            );

            double finalRating = rating + timeWeight + engagementWeight;

            matrix.computeIfAbsent(userId, k -> new HashMap<>())
                    .merge(spotId, finalRating, Math::max);
        }

        return matrix;
    }

    private Map<ItemPair, ItemPairStats> buildCoVisitPairStats(Map<Long, Map<Long, Double>> userItemMatrix) {
        Map<ItemPair, ItemPairStats> pairStats = new HashMap<>();

        for (Map<Long, Double> userRatings : userItemMatrix.values()) {
            if (userRatings.size() < 2) {
                continue;
            }

            double userAverage = userRatings.values().stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.0);
            List<Map.Entry<Long, Double>> ratedItems = userRatings.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .collect(Collectors.toList());

            for (int i = 0; i < ratedItems.size() - 1; i++) {
                Map.Entry<Long, Double> left = ratedItems.get(i);
                double adjustedLeft = left.getValue() - userAverage;

                for (int j = i + 1; j < ratedItems.size(); j++) {
                    Map.Entry<Long, Double> right = ratedItems.get(j);
                    double adjustedRight = right.getValue() - userAverage;
                    ItemPair pair = new ItemPair(left.getKey(), right.getKey());
                    pairStats.computeIfAbsent(pair, key -> new ItemPairStats())
                            .add(adjustedLeft, adjustedRight);
                }
            }
        }

        return pairStats;
    }

    private Map<Long, Map<Long, Double>> buildSimilarityMatrix(
            Set<Long> allSpotIds,
            Map<ItemPair, ItemPairStats> pairStats) {
        Map<Long, Map<Long, Double>> matrix = new ConcurrentHashMap<>();
        allSpotIds.forEach(spotId -> matrix.put(spotId, new HashMap<>()));

        for (Map.Entry<ItemPair, ItemPairStats> entry : pairStats.entrySet()) {
            ItemPair pair = entry.getKey();
            ItemPairStats stats = entry.getValue();
            if (stats.getCommonUsers() < 2) {
                continue;
            }

            double similarity = stats.calculateSimilarity();
            if (similarity < MIN_ITEM_SIMILARITY) {
                continue;
            }

            matrix.computeIfAbsent(pair.left(), key -> new HashMap<>())
                    .put(pair.right(), similarity);
            matrix.computeIfAbsent(pair.right(), key -> new HashMap<>())
                    .put(pair.left(), similarity);
        }

        return matrix.entrySet().stream()
                .collect(Collectors.toConcurrentMap(
                        Map.Entry::getKey,
                        entry -> trimSimilarItems(entry.getValue())
                ));
    }

    private Map<Long, Double> trimSimilarItems(Map<Long, Double> similarities) {
        if (similarities.isEmpty()) {
            return Collections.emptyMap();
        }

        return similarities.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(MAX_SIMILAR_ITEMS)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (v1, v2) -> v1,
                        LinkedHashMap::new
                ));
    }

    /**
     * 基于物品的推荐
     */
    public Map<Long, Double> recommendByItemCF(Long userId, Set<Long> visitedSpotIds) {
        if (visitedSpotIds == null || visitedSpotIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Map<Long, Double>> matrixSnapshot = itemSimilarityMatrix;
        if (matrixSnapshot.isEmpty() && loadSimilarityMatrixFromCache()) {
            matrixSnapshot = itemSimilarityMatrix;
        }
        if (matrixSnapshot.isEmpty()) {
            logger.warn("Item similarity matrix empty; starting precompute");
            precomputeItemSimilarityMatrix();
            matrixSnapshot = itemSimilarityMatrix;
        }

        // 获取用户的历史访问记录（用于评分权重）
        List<UserVisitHistory> userHistories = historyRepository.findByUserId(userId);
        Map<Long, Double> userRatings = userHistories.stream()
                .filter(h -> h.getSpot() != null && h.getSpot().getId() != null)
                .collect(Collectors.toMap(
                    h -> h.getSpot().getId(),
                    h -> normalizeRating(h.getRating()) +
                         calculateTimeWeight(h.getVisitDate()) +
                         calculateEngagementWeight(h.getClickCount(), h.getDwellSeconds()),
                    (v1, v2) -> Math.max(v1 != null ? v1 : 0.0, v2 != null ? v2 : 0.0)
                ));

        // 计算推荐得分
        Map<Long, Double> itemScores = new HashMap<>();

        for (Long visitedSpotId : visitedSpotIds) {
            // 获取用户对该景点的评分权重
            double userRating = userRatings.getOrDefault(visitedSpotId, 3.0);

            // 获取与该景点相似的景点
            Map<Long, Double> similarSpots = matrixSnapshot.get(visitedSpotId);
            if (similarSpots == null || similarSpots.isEmpty()) {
                continue;
            }

            // 计算推荐得分
            for (Map.Entry<Long, Double> entry : similarSpots.entrySet()) {
                Long similarSpotId = entry.getKey();
                double similarity = entry.getValue();

                // 跳过已访问的景点
                if (visitedSpotIds.contains(similarSpotId)) {
                    continue;
                }

                // 得分 = 相似度 × 用户对该景点的评分权重
                double score = similarity * userRating;
                itemScores.merge(similarSpotId, score, Double::sum);
            }
        }

        logger.info("Item-Based CF candidates generated: candidateCount={}", itemScores.size());

        return itemScores;
    }

    /**
     * 获取与指定景点相似的景点
     */
    public Map<Long, Double> getSimilarSpots(Long spotId, int limit) {
        Map<Long, Double> similarSpots = itemSimilarityMatrix.get(spotId);
        if (similarSpots == null || similarSpots.isEmpty()) {
            return Collections.emptyMap();
        }

        return similarSpots.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (v1, v2) -> v1,
                    LinkedHashMap::new
                ));
    }

    /**
     * 检查相似度矩阵是否需要更新
     */
    public boolean isSimilarityMatrixStale() {
        if (itemSimilarityMatrix.isEmpty() || itemSimilarityMatrixUpdatedAt == 0L) {
            return true;
        }
        return System.currentTimeMillis() - itemSimilarityMatrixUpdatedAt > SIMILARITY_MATRIX_TTL_MS;
    }

    // 辅助方法（与RecommendationService中的方法类似）
    private double normalizeRating(Integer rating) {
        return rating == null ? 3.0 : rating.doubleValue();
    }

    private double calculateTimeWeight(LocalDateTime visitDate) {
        if (visitDate == null) {
            return 0.0;
        }
        long days = Math.max(0, Duration.between(visitDate, LocalDateTime.now()).toDays());
        return Math.pow(0.95, days / 30.0); // 指数衰减
    }

    private double calculateEngagementWeight(Integer clickCount, Integer dwellSeconds) {
        double clickWeight = clickCount == null ? 0.0 : Math.log1p(clickCount) * 0.1;
        double dwellWeight = dwellSeconds == null ? 0.0 : Math.min(dwellSeconds / 60.0, 5.0) * 0.05;
        return clickWeight + dwellWeight;
    }

    private void saveSimilarityMatrixToCache(Map<Long, Map<Long, Double>> matrix, long updatedAt) {
        if (redisTemplate == null) {
            return;
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("updatedAt", updatedAt);
        payload.put("matrix", matrix);
        try {
            redisTemplate.opsForValue().set(
                    REDIS_MATRIX_KEY,
                    objectMapper.writeValueAsString(payload),
                    REDIS_MATRIX_TTL_HOURS,
                    TimeUnit.HOURS);
        } catch (RuntimeException ex) {
            logRedisCacheFailure("set item similarity matrix", ex);
        } catch (Exception ex) {
            logRedisCacheFailure("set item similarity matrix",
                    new IllegalStateException("Failed to serialize item similarity matrix", ex));
        }
    }

    private String getRedisValue(String key, String operation) {
        if (redisTemplate == null) {
            return null;
        }
        try {
            return redisTemplate.opsForValue().get(key);
        } catch (RuntimeException ex) {
            logRedisCacheFailure(operation, ex);
            return null;
        }
    }

    private void logRedisCacheFailure(String operation, RuntimeException ex) {
        if (!redisCacheWarningLogged) {
            redisCacheWarningLogged = true;
            logger.warn("Redis item similarity cache unavailable; using local matrix only. operation={}, error={}",
                    operation, RecommendationLogPrivacy.exceptionSummary(ex));
        } else {
            logger.debug("Redis item similarity cache operation failed: operation={}, error={}",
                    operation, RecommendationLogPrivacy.exceptionSummary(ex));
        }
    }

    private Map<Long, Map<Long, Double>> toSimilarityMatrix(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        Map<String, Object> source;
        try {
            source = objectMapper.readValue(value, JSON_OBJECT);
        } catch (Exception ex) {
            logRedisCacheFailure("parse item similarity matrix",
                    new IllegalStateException("Invalid item similarity matrix payload", ex));
            return null;
        }

        Object matrixValue = source.containsKey("matrix") ? source.get("matrix") : source;
        if (!(matrixValue instanceof Map<?, ?> matrixSource)) {
            return null;
        }

        Map<Long, Map<Long, Double>> matrix = new ConcurrentHashMap<>();
        for (Map.Entry<?, ?> rowEntry : matrixSource.entrySet()) {
            Long spotId = toLong(rowEntry.getKey());
            if (spotId == null || !(rowEntry.getValue() instanceof Map<?, ?> rowSource)) {
                continue;
            }

            Map<Long, Double> row = new LinkedHashMap<>();
            for (Map.Entry<?, ?> scoreEntry : rowSource.entrySet()) {
                Long similarSpotId = toLong(scoreEntry.getKey());
                Double score = toDouble(scoreEntry.getValue());
                if (similarSpotId != null && score != null) {
                    row.put(similarSpotId, score);
                }
            }
            matrix.put(spotId, row);
        }
        return matrix;
    }

    private Optional<Long> extractUpdatedAt(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        Map<String, Object> source;
        try {
            source = objectMapper.readValue(value, JSON_OBJECT);
        } catch (Exception ex) {
            return Optional.empty();
        }
        return Optional.ofNullable(toLong(source.get("updatedAt")));
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private Double toDouble(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String text) {
            try {
                return Double.parseDouble(text);
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }

    private record ItemPair(Long left, Long right) {
    }

    private static class ItemPairStats {
        private double sumProduct;
        private double sumSqLeft;
        private double sumSqRight;
        private int commonUsers;

        void add(double adjustedLeft, double adjustedRight) {
            sumProduct += adjustedLeft * adjustedRight;
            sumSqLeft += adjustedLeft * adjustedLeft;
            sumSqRight += adjustedRight * adjustedRight;
            commonUsers++;
        }

        int getCommonUsers() {
            return commonUsers;
        }

        double calculateSimilarity() {
            double denominator = Math.sqrt(sumSqLeft) * Math.sqrt(sumSqRight);
            return denominator == 0.0 ? 0.0 : sumProduct / denominator;
        }
    }
}
