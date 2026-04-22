package com.tibet.tourism.service;

import com.tibet.tourism.entity.ScenicSpot;
import com.tibet.tourism.entity.User;
import com.tibet.tourism.entity.UserVisitHistory;
import com.tibet.tourism.repository.ScenicSpotRepository;
import com.tibet.tourism.repository.UserRepository;
import com.tibet.tourism.repository.UserVisitHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 冷启动优化服务
 * 处理新用户和新物品的冷启动问题
 */
@Service
public class ColdStartOptimizationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ColdStartOptimizationService.class);
    
    // 冷启动阈值
    private static final int NEW_USER_THRESHOLD = 3; // 访问记录少于3条视为新用户
    private static final int NEW_ITEM_THRESHOLD = 5; // 访问记录少于5条视为新物品
    
    // 热点景点缓存（避免每次请求都调用 findAll）
    private final Map<String, List<ScenicSpot>> popularSpotsCache = new ConcurrentHashMap<>();
    private volatile long popularSpotsCacheTime = 0;
    private static final long CACHE_TTL_MS = 5 * 60 * 1000; // 5分钟缓存
    
    @Autowired
    private ScenicSpotRepository spotRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private UserVisitHistoryRepository historyRepository;
    
    @Autowired
    private ItemBasedRecommendationService itemBasedRecommendationService;
    
    /**
     * 判断用户是否为新用户（冷启动状态）
     */
    public boolean isNewUser(Long userId) {
        List<UserVisitHistory> history = historyRepository.findByUserId(userId);
        return history == null || history.size() < NEW_USER_THRESHOLD;
    }
    
    /**
     * 判断景点是否为新物品（冷启动状态）
     */
    public boolean isNewItem(Long spotId) {
        try {
            List<UserVisitHistory> history = historyRepository.findBySpotId(spotId);
            return history == null || history.size() < NEW_ITEM_THRESHOLD;
        } catch (Exception e) {
            logger.warn("检查新物品状态失败: spotId={}, error={}", spotId, e.getMessage());
            return false;
        }
    }
    
    /**
     * 新用户冷启动推荐
     * 策略1：基于用户属性的推荐（城市、IP地址等）
     */
    public List<ScenicSpot> recommendForNewUserByAttributes(Long userId) {
        logger.info("新用户冷启动推荐（基于用户属性）: userId={}", userId);
        
        Optional<User> userOpt = userRepository.findById(userId);
        if (!userOpt.isPresent()) {
            return Collections.emptyList();
        }
        
        User user = userOpt.get();
        List<ScenicSpot> recommendations = new ArrayList<>();
        
        // 策略1：基于用户所在城市推荐（在Java层过滤，因为城市是字符串匹配）
        if (user.getCity() != null && !user.getCity().isEmpty()) {
            List<ScenicSpot> allSpots = getCachedSpotList();
            List<ScenicSpot> citySpots = allSpots.stream()
                    .filter(spot -> spot.getLocation() != null && 
                            spot.getLocation().contains(user.getCity()))
                    .sorted(Comparator.comparing(ScenicSpot::getVisitCount).reversed())
                    .limit(5)
                    .collect(Collectors.toList());
            recommendations.addAll(citySpots);
            logger.info("基于城市 {} 推荐 {} 个景点", user.getCity(), citySpots.size());
        }
        
        // 策略2：基于热门景点（如果城市推荐不足）
        if (recommendations.size() < 5) {
            List<ScenicSpot> popularSpots = getCachedPopularSpots(10 - recommendations.size());
            recommendations.addAll(popularSpots);
            logger.info("补充热门景点 {} 个", popularSpots.size());
        }
        
        // 去重
        return recommendations.stream()
                .distinct()
                .limit(10)
                .collect(Collectors.toList());
    }
    
    /**
     * 新用户冷启动推荐
     * 策略2：基于用户偏好问卷（如果用户填写了问卷）
     */
    public List<ScenicSpot> recommendForNewUserByPreferences(
            Long userId, 
            List<String> preferredTags,
            String preferredCategory,
            String companionType) {
        
        logger.info("新用户冷启动推荐（基于偏好问卷）: userId={}, tags={}, category={}, companion={}", 
                userId, preferredTags, preferredCategory, companionType);
        
        List<ScenicSpot> candidates = new ArrayList<>();
        
        // 基于标签匹配（使用缓存的景点列表）
        if (preferredTags != null && !preferredTags.isEmpty()) {
            candidates.addAll(findSpotsByTags(preferredTags, 10));
        }
        
        // 基于类别匹配
        if (preferredCategory != null) {
            try {
                ScenicSpot.Category category = ScenicSpot.Category.valueOf(preferredCategory.toUpperCase());
                List<ScenicSpot> categorySpots = spotRepository.findByCategory(category);
                candidates.addAll(categorySpots);
            } catch (IllegalArgumentException e) {
                logger.warn("无效的类别: {}", preferredCategory);
            }
        }
        
        // 基于旅伴类型过滤
        if (companionType != null) {
            candidates = filterByCompanionType(candidates, companionType);
        }
        
        // 如果候选不足，补充热门景点
        if (candidates.size() < 10) {
            List<ScenicSpot> popularSpots = getCachedPopularSpots(10 - candidates.size());
            candidates.addAll(popularSpots);
        }
        
        // 去重并排序
        return candidates.stream()
                .distinct()
                .sorted(Comparator.comparing(ScenicSpot::getVisitCount).reversed())
                .limit(10)
                .collect(Collectors.toList());
    }
    
    /**
     * 新用户冷启动推荐
     * 策略3：基于位置的推荐（如果提供了当前位置）
     */
    public List<ScenicSpot> recommendForNewUserByLocation(
            Double latitude, 
            Double longitude, 
            Double maxDistanceKm) {
        
        logger.info("新用户冷启动推荐（基于位置）: lat={}, lng={}, maxDistance={}km", 
                latitude, longitude, maxDistanceKm);
        
        if (latitude == null || longitude == null) {
            return Collections.emptyList();
        }
        
        double maxDistance = maxDistanceKm != null ? maxDistanceKm : 50.0;
        
        List<ScenicSpot> allSpots = getCachedSpotList();
        
        return allSpots.stream()
                .filter(spot -> spot.getLatitude() != null && spot.getLongitude() != null)
                .map(spot -> {
                    double spotLat = spot.getLatitude().doubleValue();
                    double spotLng = spot.getLongitude().doubleValue();
                    double distance = calculateDistance(latitude, longitude, spotLat, spotLng);
                    return new SpotWithDistance(spot, distance);
                })
                .filter(s -> s.distance <= maxDistance)
                .sorted(Comparator.<SpotWithDistance>comparingDouble(s -> s.distance)
                        .thenComparing((SpotWithDistance s) -> s.spot.getVisitCount() != null ? s.spot.getVisitCount() : 0, Comparator.reverseOrder()))
                .limit(10)
                .map(s -> s.spot)
                .collect(Collectors.toList());
    }
    
    /**
     * 新物品冷启动推荐
     * 基于内容相似度推荐新景点
     */
    public List<ScenicSpot> recommendNewItems(Long userId) {
        logger.info("新物品冷启动推荐: userId={}", userId);
        
        // 使用缓存的景点列表，只调用一次
        List<ScenicSpot> allSpots = getCachedSpotList();
        List<ScenicSpot> newItems = allSpots.stream()
                .filter(spot -> isNewItem(spot.getId()))
                .collect(Collectors.toList());
        
        if (newItems.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 基于内容相似度排序
        List<UserVisitHistory> userHistory = historyRepository.findByUserId(userId);
        if (!userHistory.isEmpty()) {
            // 获取用户偏好的标签
            Set<String> userTags = userHistory.stream()
                    .flatMap(h -> {
                        if (h.getSpot() != null && h.getSpot().getTags() != null) {
                            return h.getSpot().getTags().stream()
                                    .map(tag -> tag.getTag());
                        }
                        return Collections.<String>emptySet().stream();
                    })
                    .collect(Collectors.toSet());
            
            return newItems.stream()
                    .map(spot -> {
                        double score = calculateContentSimilarity(spot, userTags);
                        return new SpotWithScore(spot, score);
                    })
                    .sorted(Comparator.comparing((SpotWithScore s) -> s.score).reversed())
                    .limit(10)
                    .map(s -> s.spot)
                    .collect(Collectors.toList());
        }
        
        return newItems.stream()
                .sorted(Comparator
                        .<ScenicSpot>comparingDouble(s -> s.getRating() != null ? s.getRating().doubleValue() : 0.0)
                        .reversed()
                        .thenComparingInt(s -> s.getVisitCount() != null ? s.getVisitCount() : 0)
                        .reversed())
                .limit(10)
                .collect(Collectors.toList());
    }
    
    /**
     * 混合冷启动推荐（综合多种策略）
     */
    public List<ScenicSpot> hybridColdStartRecommendation(
            Long userId,
            Double latitude,
            Double longitude,
            List<String> preferredTags,
            String preferredCategory,
            String companionType) {
        
        logger.info("混合冷启动推荐: userId={}", userId);
        
        Map<Long, Double> candidateScores = new HashMap<>();
        
        // 策略1：基于用户属性（30%权重）
        List<ScenicSpot> attributeBased = recommendForNewUserByAttributes(userId);
        attributeBased.forEach(spot -> {
            candidateScores.merge(spot.getId(), 0.3, Double::sum);
        });
        
        // 策略2：基于位置（30%权重）
        if (latitude != null && longitude != null) {
            List<ScenicSpot> locationBased = recommendForNewUserByLocation(latitude, longitude, 50.0);
            locationBased.forEach(spot -> {
                candidateScores.merge(spot.getId(), 0.3, Double::sum);
            });
        }
        
        // 策略3：基于偏好问卷（40%权重）
        if (preferredTags != null || preferredCategory != null) {
            List<ScenicSpot> preferenceBased = recommendForNewUserByPreferences(
                    userId, preferredTags, preferredCategory, companionType);
            preferenceBased.forEach(spot -> {
                candidateScores.merge(spot.getId(), 0.4, Double::sum);
            });
        }
        
        // 如果候选不足，补充热门景点
        if (candidateScores.size() < 10) {
            List<ScenicSpot> popularSpots = getCachedPopularSpots(10);
            popularSpots.forEach(spot -> {
                candidateScores.putIfAbsent(spot.getId(), 0.1);
            });
        }
        
        // 按得分排序
        return candidateScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(10)
                .map(entry -> spotRepository.findById(entry.getKey()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.<ScenicSpot>toList());
    }
    
    // ========== 辅助方法 ==========
    
    /**
     * 获取带缓存的景点列表（5分钟TTL）
     * 避免每次请求都调用 findAll()
     */
    private List<ScenicSpot> getCachedSpotList() {
        String cacheKey = "all";
        long now = System.currentTimeMillis();
        
        if (popularSpotsCache.containsKey(cacheKey) 
                && (now - popularSpotsCacheTime) < CACHE_TTL_MS) {
            return popularSpotsCache.get(cacheKey);
        }
        
        List<ScenicSpot> spots = spotRepository.findAll();
        popularSpotsCache.put(cacheKey, spots);
        popularSpotsCacheTime = now;
        return spots;
    }
    
    /**
     * 获取热门景点（从缓存列表中取前N个）
     */
    private List<ScenicSpot> getCachedPopularSpots(int limit) {
        List<ScenicSpot> allSpots = getCachedSpotList();
        return allSpots.stream()
                .sorted(Comparator
                        .<ScenicSpot>comparingInt(s -> s.getVisitCount() != null ? s.getVisitCount() : 0)
                        .reversed()
                        .thenComparingDouble(s -> s.getRating() != null ? s.getRating().doubleValue() : 0.0)
                        .reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    private List<ScenicSpot> findSpotsByTags(List<String> tags, int limit) {
        List<ScenicSpot> allSpots = getCachedSpotList();
        Set<String> tagSet = new HashSet<>(tags);
        return allSpots.stream()
                .filter(spot -> {
                    if (spot.getTags() == null) return false;
                    Set<String> spotTags = spot.getTags().stream()
                            .map(t -> t.getTag())
                            .collect(Collectors.toSet());
                    return tagSet.stream().anyMatch(spotTags::contains);
                })
                .sorted(Comparator.comparing(ScenicSpot::getVisitCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
    
    private List<ScenicSpot> filterByCompanionType(List<ScenicSpot> spots, String companionType) {
        return spots.stream()
                .filter(spot -> true) // 占位：可基于景点标签或属性扩展
                .collect(Collectors.toList());
    }
    
    private double calculateContentSimilarity(ScenicSpot spot, Set<String> userTags) {
        if (spot.getTags() == null || userTags.isEmpty()) {
            return 0.0;
        }
        
        Set<String> spotTags = spot.getTags().stream()
                .map(t -> t.getTag())
                .collect(Collectors.toSet());
        
        Set<String> intersection = new HashSet<>(userTags);
        intersection.retainAll(spotTags);
        
        Set<String> union = new HashSet<>(userTags);
        union.addAll(spotTags);
        
        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }
    
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
    
    private static class SpotWithDistance {
        ScenicSpot spot;
        double distance;
        
        SpotWithDistance(ScenicSpot spot, double distance) {
            this.spot = spot;
            this.distance = distance;
        }
    }
    
    private static class SpotWithScore {
        ScenicSpot spot;
        double score;
        
        SpotWithScore(ScenicSpot spot, double score) {
            this.spot = spot;
            this.score = score;
        }
    }
}
