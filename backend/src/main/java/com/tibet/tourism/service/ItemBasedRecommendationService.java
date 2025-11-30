package com.tibet.tourism.service;

import com.tibet.tourism.entity.ScenicSpot;
import com.tibet.tourism.entity.UserVisitHistory;
import com.tibet.tourism.repository.ScenicSpotRepository;
import com.tibet.tourism.repository.UserVisitHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 基于物品的协同过滤推荐服务
 * 计算景点之间的相似度，基于用户访问历史推荐相似景点
 */
@Service
public class ItemBasedRecommendationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ItemBasedRecommendationService.class);
    
    // 景点相似度矩阵缓存
    private final Map<Long, Map<Long, Double>> itemSimilarityMatrix = new ConcurrentHashMap<>();
    private static final double MIN_ITEM_SIMILARITY = 0.1; // 最小景点相似度阈值
    private static final int MAX_SIMILAR_ITEMS = 50; // 每个景点最多保留的相似景点数
    
    @Autowired
    private ScenicSpotRepository spotRepository;
    
    @Autowired
    private UserVisitHistoryRepository historyRepository;
    
    /**
     * 预计算景点相似度矩阵（离线计算）
     * 建议每天凌晨执行一次
     */
    public void precomputeItemSimilarityMatrix() {
        logger.info("═══════════════════════════════════════════════════════════");
        logger.info("🔄 开始预计算景点相似度矩阵");
        logger.info("═══════════════════════════════════════════════════════════");
        
        long startTime = System.currentTimeMillis();
        itemSimilarityMatrix.clear();
        
        // 获取所有景点
        List<ScenicSpot> allSpots = spotRepository.findAll();
        logger.info("📊 景点总数: {}", allSpots.size());
        
        // 获取所有用户访问记录，构建用户-景点评分矩阵
        List<UserVisitHistory> allHistories = historyRepository.findAll();
        Map<Long, Map<Long, Double>> userItemMatrix = buildUserItemMatrix(allHistories);
        logger.info("📊 用户-景点矩阵: {} 用户 × {} 景点", 
                userItemMatrix.size(), 
                allSpots.size());
        
        // 并行计算景点相似度
        Map<Long, Map<Long, Double>> similarityMatrix = allSpots.parallelStream()
                .collect(Collectors.toConcurrentMap(
                    ScenicSpot::getId,
                    spot1 -> {
                        Map<Long, Double> similarities = new HashMap<>();
                        
                        for (ScenicSpot spot2 : allSpots) {
                            if (spot1.getId().equals(spot2.getId())) {
                                continue; // 跳过自己
                            }
                            
                            double similarity = calculateItemSimilarity(
                                    spot1.getId(), 
                                    spot2.getId(), 
                                    userItemMatrix
                            );
                            
                            if (similarity >= MIN_ITEM_SIMILARITY) {
                                similarities.put(spot2.getId(), similarity);
                            }
                        }
                        
                        // 只保留相似度最高的N个
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
                ));
        
        itemSimilarityMatrix.putAll(similarityMatrix);
        
        long endTime = System.currentTimeMillis();
        logger.info("✅ 景点相似度矩阵计算完成，耗时: {}ms", endTime - startTime);
        logger.info("📊 平均每个景点有 {} 个相似景点", 
                itemSimilarityMatrix.values().stream()
                        .mapToInt(Map::size)
                        .average()
                        .orElse(0.0));
        logger.info("═══════════════════════════════════════════════════════════\n");
    }
    
    /**
     * 构建用户-景点评分矩阵
     */
    private Map<Long, Map<Long, Double>> buildUserItemMatrix(List<UserVisitHistory> histories) {
        Map<Long, Map<Long, Double>> matrix = new HashMap<>();
        
        for (UserVisitHistory history : histories) {
            if (history.getUser() == null || history.getSpot() == null) {
                continue;
            }
            
            Long userId = history.getUser().getId();
            Long spotId = history.getSpot().getId();
            
            // 计算评分（考虑评分、时间衰减、行为权重）
            double rating = normalizeRating(history.getRating());
            double timeWeight = calculateTimeWeight(history.getVisitDate());
            double engagementWeight = calculateEngagementWeight(
                    history.getClickCount(), 
                    history.getDwellSeconds()
            );
            
            double finalRating = rating + timeWeight + engagementWeight;
            
            matrix.computeIfAbsent(userId, k -> new HashMap<>())
                    .put(spotId, finalRating);
        }
        
        return matrix;
    }
    
    /**
     * 计算两个景点之间的相似度（使用调整后的余弦相似度）
     */
    private double calculateItemSimilarity(
            Long spotId1, 
            Long spotId2, 
            Map<Long, Map<Long, Double>> userItemMatrix) {
        
        // 找到同时访问过这两个景点的用户
        Set<Long> commonUsers = new HashSet<>();
        
        for (Long userId : userItemMatrix.keySet()) {
            Map<Long, Double> userRatings = userItemMatrix.get(userId);
            if (userRatings != null 
                && userRatings.containsKey(spotId1) 
                && userRatings.containsKey(spotId2)) {
                commonUsers.add(userId);
            }
        }
        
        if (commonUsers.size() < 2) {
            return 0.0; // 至少需要2个共同用户
        }
        
        // 计算调整后的余弦相似度
        double sumProduct = 0.0;
        double sumSq1 = 0.0, sumSq2 = 0.0;
        
        // 计算两个景点的平均评分
        double avgRating1 = commonUsers.stream()
                .mapToDouble(userId -> userItemMatrix.get(userId).get(spotId1))
                .average()
                .orElse(0.0);
        
        double avgRating2 = commonUsers.stream()
                .mapToDouble(userId -> userItemMatrix.get(userId).get(spotId2))
                .average()
                .orElse(0.0);
        
        // 计算调整后的余弦相似度
        for (Long userId : commonUsers) {
            Map<Long, Double> userRatings = userItemMatrix.get(userId);
            double rating1 = userRatings.get(spotId1) - avgRating1;
            double rating2 = userRatings.get(spotId2) - avgRating2;
            
            sumProduct += rating1 * rating2;
            sumSq1 += rating1 * rating1;
            sumSq2 += rating2 * rating2;
        }
        
        double denominator = Math.sqrt(sumSq1) * Math.sqrt(sumSq2);
        if (denominator == 0.0) {
            return 0.0;
        }
        
        return sumProduct / denominator;
    }
    
    /**
     * 基于物品的推荐
     */
    public Map<Long, Double> recommendByItemCF(Long userId, Set<Long> visitedSpotIds) {
        if (visitedSpotIds == null || visitedSpotIds.isEmpty()) {
            return Collections.emptyMap();
        }
        
        // 如果相似度矩阵为空，先计算
        if (itemSimilarityMatrix.isEmpty()) {
            logger.warn("⚠️  景点相似度矩阵为空，开始预计算...");
            precomputeItemSimilarityMatrix();
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
            Map<Long, Double> similarSpots = itemSimilarityMatrix.get(visitedSpotId);
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
        
        logger.info("📊 Item-Based CF生成 {} 个候选景点", itemScores.size());
        
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
        // 如果矩阵为空，需要更新
        if (itemSimilarityMatrix.isEmpty()) {
            return true;
        }
        
        // 可以添加时间戳检查逻辑
        // 例如：如果超过24小时未更新，返回true
        return false;
    }
    
    // 辅助方法（与RecommendationService中的方法类似）
    private double normalizeRating(Integer rating) {
        return rating == null ? 3.0 : rating.doubleValue();
    }
    
    private double calculateTimeWeight(java.time.LocalDateTime visitDate) {
        if (visitDate == null) {
            return 0.0;
        }
        long days = Math.max(0, java.time.Duration.between(visitDate, java.time.LocalDateTime.now()).toDays());
        return Math.pow(0.95, days / 30.0); // 指数衰减
    }
    
    private double calculateEngagementWeight(Integer clickCount, Integer dwellSeconds) {
        double clickWeight = clickCount == null ? 0.0 : Math.log1p(clickCount) * 0.1;
        double dwellWeight = dwellSeconds == null ? 0.0 : Math.min(dwellSeconds / 60.0, 5.0) * 0.05;
        return clickWeight + dwellWeight;
    }
}

