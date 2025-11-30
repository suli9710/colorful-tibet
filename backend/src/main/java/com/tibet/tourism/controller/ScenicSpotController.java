package com.tibet.tourism.controller;

import com.tibet.tourism.dto.RecommendationContext;
import com.tibet.tourism.dto.RecommendationDebugResponse;
import com.tibet.tourism.entity.ScenicSpot;
import com.tibet.tourism.dto.UserPreferenceDTO;
import com.tibet.tourism.service.ColdStartOptimizationService;
import com.tibet.tourism.service.CompanionInferenceService;
import com.tibet.tourism.service.ItemBasedRecommendationService;
import com.tibet.tourism.service.RecommendationService;
import com.tibet.tourism.service.ScenicSpotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/spots")
@CrossOrigin(origins = "*")
public class ScenicSpotController {

    @Autowired
    private ScenicSpotService scenicSpotService;

    @Autowired
    private RecommendationService recommendationService;
    
    @Autowired
    private CompanionInferenceService companionInferenceService;
    
    @Autowired
    private ItemBasedRecommendationService itemBasedRecommendationService;
    
    @Autowired
    private ColdStartOptimizationService coldStartOptimizationService;

    @GetMapping
    public List<ScenicSpot> getAllSpots(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "zh") String locale) {
        List<ScenicSpot> spots;
        if (category != null) {
            spots = scenicSpotService.getSpotsByCategory(ScenicSpot.Category.valueOf(category.toUpperCase()));
        } else {
            spots = scenicSpotService.getAllSpots();
        }
        
        // 根据语言设置返回相应的字段
        if ("bo".equals(locale)) {
            spots.forEach(spot -> {
                if (spot.getNameTibetan() != null && !spot.getNameTibetan().isEmpty()) {
                    spot.setName(spot.getNameTibetan());
                }
                if (spot.getDescriptionTibetan() != null && !spot.getDescriptionTibetan().isEmpty()) {
                    spot.setDescription(spot.getDescriptionTibetan());
                }
            });
        }
        return spots;
    }

    @GetMapping("/{id}")
    public ScenicSpot getSpotById(@PathVariable Long id, @RequestParam(required = false, defaultValue = "zh") String locale) {
        ScenicSpot spot = scenicSpotService.getSpotById(id);
        if (spot != null && "bo".equals(locale)) {
            if (spot.getNameTibetan() != null && !spot.getNameTibetan().isEmpty()) {
                spot.setName(spot.getNameTibetan());
            }
            if (spot.getDescriptionTibetan() != null && !spot.getDescriptionTibetan().isEmpty()) {
                spot.setDescription(spot.getDescriptionTibetan());
            }
        }
        return spot;
    }

    @GetMapping("/search")
    public List<ScenicSpot> searchSpots(
            @RequestParam String keyword,
            @RequestParam(required = false, defaultValue = "zh") String locale) {
        List<ScenicSpot> spots = scenicSpotService.searchSpots(keyword);
        if ("bo".equals(locale)) {
            spots.forEach(spot -> {
                if (spot.getNameTibetan() != null && !spot.getNameTibetan().isEmpty()) {
                    spot.setName(spot.getNameTibetan());
                }
                if (spot.getDescriptionTibetan() != null && !spot.getDescriptionTibetan().isEmpty()) {
                    spot.setDescription(spot.getDescriptionTibetan());
                }
            });
        }
        return spots;
    }

    @GetMapping("/recommendations")
    public List<ScenicSpot> getRecommendations(
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "zh") String locale,
            // 上下文参数（可选）
            @RequestParam(required = false) String season,
            @RequestParam(required = false) String weather,
            @RequestParam(required = false) String currentLocation,
            @RequestParam(required = false) Double currentLatitude,
            @RequestParam(required = false) Double currentLongitude,
            @RequestParam(required = false) String timeOfDay,
            @RequestParam(required = false) String companion,
            @RequestParam(required = false) Integer budget,
            @RequestParam(required = false) Integer travelDays,
            @RequestParam(required = false) String preferredActivities,
            @RequestParam(required = false, defaultValue = "true") Boolean considerDistance,
            @RequestParam(required = false, defaultValue = "true") Boolean considerBudget) {
        
        // 构建上下文对象
        RecommendationContext context = null;
        if (season != null || weather != null || currentLocation != null || 
            currentLatitude != null || currentLongitude != null || 
            timeOfDay != null || companion != null || budget != null || 
            travelDays != null || preferredActivities != null) {
            context = new RecommendationContext();
            context.setSeason(season);
            context.setWeather(weather);
            context.setCurrentLocation(currentLocation);
            context.setCurrentLatitude(currentLatitude);
            context.setCurrentLongitude(currentLongitude);
            context.setTimeOfDay(timeOfDay);
            context.setCompanion(companion);
            context.setBudget(budget);
            context.setTravelDays(travelDays);
            context.setPreferredActivities(preferredActivities);
            context.setConsiderDistance(considerDistance);
            context.setConsiderBudget(considerBudget);
        }
        
        List<ScenicSpot> spots = recommendationService.recommendSpotsForUser(userId, context);
        if ("bo".equals(locale)) {
            spots.forEach(spot -> {
                if (spot.getNameTibetan() != null && !spot.getNameTibetan().isEmpty()) {
                    spot.setName(spot.getNameTibetan());
                }
                if (spot.getDescriptionTibetan() != null && !spot.getDescriptionTibetan().isEmpty()) {
                    spot.setDescription(spot.getDescriptionTibetan());
                }
            });
        }
        return spots;
    }
    
    @PostMapping("/recommendations")
    public List<ScenicSpot> getRecommendationsWithContext(
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "zh") String locale,
            @RequestBody(required = false) RecommendationContext context) {
        
        List<ScenicSpot> spots = recommendationService.recommendSpotsForUser(userId, context);
        if ("bo".equals(locale)) {
            spots.forEach(spot -> {
                if (spot.getNameTibetan() != null && !spot.getNameTibetan().isEmpty()) {
                    spot.setName(spot.getNameTibetan());
                }
                if (spot.getDescriptionTibetan() != null && !spot.getDescriptionTibetan().isEmpty()) {
                    spot.setDescription(spot.getDescriptionTibetan());
                }
            });
        }
        return spots;
    }

    @GetMapping("/recommendations/debug")
    public RecommendationDebugResponse getRecommendationDebug(
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "zh") String locale,
            // 上下文参数（可选）
            @RequestParam(required = false) String season,
            @RequestParam(required = false) String weather,
            @RequestParam(required = false) String currentLocation,
            @RequestParam(required = false) Double currentLatitude,
            @RequestParam(required = false) Double currentLongitude,
            @RequestParam(required = false) String timeOfDay,
            @RequestParam(required = false) String companion,
            @RequestParam(required = false) Integer budget,
            @RequestParam(required = false) Integer travelDays,
            @RequestParam(required = false) String preferredActivities,
            @RequestParam(required = false, defaultValue = "true") Boolean considerDistance,
            @RequestParam(required = false, defaultValue = "true") Boolean considerBudget) {
        
        // 构建上下文对象
        RecommendationContext context = null;
        if (season != null || weather != null || currentLocation != null || 
            currentLatitude != null || currentLongitude != null || 
            timeOfDay != null || companion != null || budget != null || 
            travelDays != null || preferredActivities != null) {
            context = new RecommendationContext();
            context.setSeason(season);
            context.setWeather(weather);
            context.setCurrentLocation(currentLocation);
            context.setCurrentLatitude(currentLatitude);
            context.setCurrentLongitude(currentLongitude);
            context.setTimeOfDay(timeOfDay);
            context.setCompanion(companion);
            context.setBudget(budget);
            context.setTravelDays(travelDays);
            context.setPreferredActivities(preferredActivities);
            context.setConsiderDistance(considerDistance);
            context.setConsiderBudget(considerBudget);
        }
        
        RecommendationDebugResponse response = recommendationService.recommendWithDebug(userId, context);
        if ("bo".equals(locale) && response.getRecommendations() != null) {
            response.getRecommendations().forEach(spot -> {
                if (spot.getNameTibetan() != null && !spot.getNameTibetan().isEmpty()) {
                    spot.setName(spot.getNameTibetan());
                }
                if (spot.getDescriptionTibetan() != null && !spot.getDescriptionTibetan().isEmpty()) {
                    spot.setDescription(spot.getDescriptionTibetan());
                }
            });
        }
        return response;
    }
    
    @PostMapping("/recommendations/debug")
    public RecommendationDebugResponse getRecommendationDebugWithContext(
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "zh") String locale,
            @RequestBody(required = false) RecommendationContext context) {
        
        RecommendationDebugResponse response = recommendationService.recommendWithDebug(userId, context);
        if ("bo".equals(locale) && response.getRecommendations() != null) {
            response.getRecommendations().forEach(spot -> {
                if (spot.getNameTibetan() != null && !spot.getNameTibetan().isEmpty()) {
                    spot.setName(spot.getNameTibetan());
                }
                if (spot.getDescriptionTibetan() != null && !spot.getDescriptionTibetan().isEmpty()) {
                    spot.setDescription(spot.getDescriptionTibetan());
                }
            });
        }
        return response;
    }
    
    /**
     * 获取用户推断的旅伴类型
     */
    @GetMapping("/companion-type")
    public Map<String, Object> getCompanionType(@RequestParam Long userId) {
        return companionInferenceService.getCompanionTypeWithConfidence(userId);
    }
    
    /**
     * 预计算景点相似度矩阵（管理员接口）
     * 建议每天凌晨执行一次
     */
    @PostMapping("/admin/precompute-similarity")
    public Map<String, Object> precomputeItemSimilarity() {
        long startTime = System.currentTimeMillis();
        try {
            itemBasedRecommendationService.precomputeItemSimilarityMatrix();
            long endTime = System.currentTimeMillis();
            return Map.of(
                "success", true,
                "message", "景点相似度矩阵计算完成",
                "duration", endTime - startTime
            );
        } catch (Exception e) {
            return Map.of(
                "success", false,
                "message", "计算失败: " + e.getMessage()
            );
        }
    }
    
    /**
     * 获取与指定景点相似的景点
     */
    @GetMapping("/{id}/similar")
    public Map<String, Object> getSimilarSpots(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "10") int limit) {
        Map<Long, Double> similarSpots = itemBasedRecommendationService.getSimilarSpots(id, limit);
        return Map.of(
            "spotId", id,
            "similarSpots", similarSpots,
            "count", similarSpots.size()
        );
    }
    
    /**
     * 新用户冷启动推荐（基于偏好问卷）
     */
    @PostMapping("/recommendations/cold-start")
    public List<ScenicSpot> getColdStartRecommendations(
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "zh") String locale,
            @RequestBody(required = false) UserPreferenceDTO preferences) {
        
        List<ScenicSpot> recommendations;
        
        if (preferences != null) {
            // 使用混合冷启动推荐
            recommendations = coldStartOptimizationService.hybridColdStartRecommendation(
                    userId,
                    preferences.getLatitude(),
                    preferences.getLongitude(),
                    preferences.getPreferredTags(),
                    preferences.getPreferredCategory(),
                    preferences.getCompanionType()
            );
        } else {
            // 使用基于用户属性的推荐
            recommendations = coldStartOptimizationService.recommendForNewUserByAttributes(userId);
        }
        
        // 根据语言设置返回相应的字段
        if ("bo".equals(locale)) {
            recommendations.forEach(spot -> {
                if (spot.getNameTibetan() != null && !spot.getNameTibetan().isEmpty()) {
                    spot.setName(spot.getNameTibetan());
                }
                if (spot.getDescriptionTibetan() != null && !spot.getDescriptionTibetan().isEmpty()) {
                    spot.setDescription(spot.getDescriptionTibetan());
                }
            });
        }
        
        return recommendations;
    }
    
    /**
     * 基于位置的冷启动推荐
     */
    @GetMapping("/recommendations/cold-start/location")
    public List<ScenicSpot> getColdStartRecommendationsByLocation(
            @RequestParam Long userId,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false, defaultValue = "50.0") Double maxDistanceKm,
            @RequestParam(required = false, defaultValue = "zh") String locale) {
        
        List<ScenicSpot> recommendations = coldStartOptimizationService
                .recommendForNewUserByLocation(latitude, longitude, maxDistanceKm);
        
        // 根据语言设置返回相应的字段
        if ("bo".equals(locale)) {
            recommendations.forEach(spot -> {
                if (spot.getNameTibetan() != null && !spot.getNameTibetan().isEmpty()) {
                    spot.setName(spot.getNameTibetan());
                }
                if (spot.getDescriptionTibetan() != null && !spot.getDescriptionTibetan().isEmpty()) {
                    spot.setDescription(spot.getDescriptionTibetan());
                }
            });
        }
        
        return recommendations;
    }
    
    /**
     * 检查用户是否为新用户
     */
    @GetMapping("/user/{userId}/is-new")
    public Map<String, Object> checkIfNewUser(@PathVariable Long userId) {
        boolean isNew = coldStartOptimizationService.isNewUser(userId);
        return Map.of(
            "userId", userId,
            "isNewUser", isNew
        );
    }
}
