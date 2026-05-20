package com.tibet.tourism.modules.spot.web;
import com.tibet.tourism.common.util.LocaleHelper;
import com.tibet.tourism.modules.recommendation.application.ColdStartOptimizationService;
import com.tibet.tourism.modules.recommendation.application.ItemBasedRecommendationService;
import com.tibet.tourism.modules.recommendation.application.RecommendationContextBuilder;
import com.tibet.tourism.modules.recommendation.application.RecommendationService;
import com.tibet.tourism.modules.recommendation.web.dto.RecommendationContext;
import com.tibet.tourism.modules.recommendation.web.dto.RecommendationDebugResponse;
import com.tibet.tourism.modules.spot.application.CompanionInferenceService;
import com.tibet.tourism.modules.spot.application.ScenicSpotService;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.web.dto.ScenicSpotHeatmapPointDTO;
import com.tibet.tourism.modules.spot.web.dto.UserPreferenceDTO;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/spots")
public class ScenicSpotController {

    private static final Logger logger = LoggerFactory.getLogger(ScenicSpotController.class);

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

    @Autowired
    private UserRepository userRepository;

    @GetMapping
    public Page<ScenicSpot> getAllSpots(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "zh") String locale,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ScenicSpot> spots;
        if (category != null) {
            spots = scenicSpotService.getSpotsByCategory(ScenicSpot.Category.valueOf(category.toUpperCase()), pageable);
        } else {
            spots = scenicSpotService.getAllSpots(pageable);
        }

        spots.forEach(spot -> LocaleHelper.resolveScenicSpotLocale(spot, locale));
        return spots;
    }

    @GetMapping("/{id}")
    public ScenicSpot getSpotById(@PathVariable Long id, @RequestParam(required = false, defaultValue = "zh") String locale) {
        ScenicSpot spot = scenicSpotService.getSpotById(id);
        LocaleHelper.resolveScenicSpotLocale(spot, locale);
        return spot;
    }

    @GetMapping("/heatmap")
    public List<ScenicSpotHeatmapPointDTO> getHeatmapSpots(
            @RequestParam(required = false, defaultValue = "zh") String locale,
            @RequestParam(required = false, defaultValue = "100") int limit) {
        return scenicSpotService.getHeatmapSpots(locale, limit);
    }

    @GetMapping("/search")
    public Page<ScenicSpot> searchSpots(
            @RequestParam String keyword,
            @RequestParam(required = false, defaultValue = "zh") String locale,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ScenicSpot> spots = scenicSpotService.searchSpots(keyword, pageable);
        spots.forEach(spot -> LocaleHelper.resolveScenicSpotLocale(spot, locale));
        return spots;
    }

    @GetMapping("/recommendations")
    @PreAuthorize("isAuthenticated()")
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
            @RequestParam(required = false, defaultValue = "true") Boolean considerBudget,
            Authentication authentication) {
        requireSelfOrAdmin(userId, authentication);
        
        RecommendationContext context = RecommendationContextBuilder.buildFromParams(
                season, weather, currentLocation, currentLatitude, currentLongitude,
                timeOfDay, companion, budget, travelDays, preferredActivities,
                considerDistance, considerBudget);
        
        List<ScenicSpot> spots = recommendationService.recommendSpotsForUser(userId, context);
        spots.forEach(spot -> LocaleHelper.resolveScenicSpotLocale(spot, locale));
        return spots;
    }
    
    @PostMapping("/recommendations")
    @PreAuthorize("isAuthenticated()")
    public List<ScenicSpot> getRecommendationsWithContext(
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "zh") String locale,
            @RequestBody(required = false) RecommendationContext context,
            Authentication authentication) {
        requireSelfOrAdmin(userId, authentication);
        
        List<ScenicSpot> spots = recommendationService.recommendSpotsForUser(userId, context);
        spots.forEach(spot -> LocaleHelper.resolveScenicSpotLocale(spot, locale));
        return spots;
    }

    @GetMapping("/recommendations/debug")
    @PreAuthorize("isAuthenticated()")
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
            @RequestParam(required = false, defaultValue = "true") Boolean considerBudget,
            Authentication authentication) {
        requireSelfOrAdmin(userId, authentication);
        
        RecommendationContext context = RecommendationContextBuilder.buildFromParams(
                season, weather, currentLocation, currentLatitude, currentLongitude,
                timeOfDay, companion, budget, travelDays, preferredActivities,
                considerDistance, considerBudget);
        
        RecommendationDebugResponse response = recommendationService.recommendWithDebug(userId, context);
        if (response.getRecommendations() != null) {
            response.getRecommendations().forEach(spot -> LocaleHelper.resolveScenicSpotLocale(spot, locale));
        }
        return response;
    }

    @PostMapping("/recommendations/debug")
    @PreAuthorize("isAuthenticated()")
    public RecommendationDebugResponse getRecommendationDebugWithContext(
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "zh") String locale,
            @RequestBody(required = false) RecommendationContext context,
            Authentication authentication) {
        requireSelfOrAdmin(userId, authentication);

        RecommendationDebugResponse response = recommendationService.recommendWithDebug(userId, context);
        if (response.getRecommendations() != null) {
            response.getRecommendations().forEach(spot -> LocaleHelper.resolveScenicSpotLocale(spot, locale));
        }
        return response;
    }
    
    /**
     * 获取用户推断的旅伴类型
     */
    @GetMapping("/companion-type")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> getCompanionType(@RequestParam Long userId, Authentication authentication) {
        requireSelfOrAdmin(userId, authentication);
        return companionInferenceService.getCompanionTypeWithConfidence(userId);
    }
    
    /**
     * 预计算景点相似度矩阵（管理员接口）
     * 建议每天凌晨执行一次
     */
    @PostMapping("/admin/precompute-similarity")
    @PreAuthorize("hasRole('ADMIN')")
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
            logger.error("Failed to precompute scenic spot similarity matrix", e);
            return Map.of(
                "success", false,
                "message", "计算失败，请稍后重试"
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
     * 新用户冷启动推荐
     */
    @PostMapping("/recommendations/cold-start")
    @PreAuthorize("isAuthenticated()")
    public List<ScenicSpot> getColdStartRecommendations(
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "zh") String locale,
            @RequestBody(required = false) UserPreferenceDTO preferences,
            Authentication authentication) {
        requireSelfOrAdmin(userId, authentication);
        
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
        
        recommendations.forEach(spot -> LocaleHelper.resolveScenicSpotLocale(spot, locale));
        return recommendations;
    }

    /**
     * 基于位置的冷启动推荐
     */
    @GetMapping("/recommendations/cold-start/location")
    @PreAuthorize("isAuthenticated()")
    public List<ScenicSpot> getColdStartRecommendationsByLocation(
            @RequestParam Long userId,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false, defaultValue = "50.0") Double maxDistanceKm,
            @RequestParam(required = false, defaultValue = "zh") String locale,
            Authentication authentication) {
        requireSelfOrAdmin(userId, authentication);

        List<ScenicSpot> recommendations = coldStartOptimizationService
                .recommendForNewUserByLocation(latitude, longitude, maxDistanceKm);
        recommendations.forEach(spot -> LocaleHelper.resolveScenicSpotLocale(spot, locale));
        return recommendations;
    }
    
    /**
     * 检查用户是否为新用户
     */
    @GetMapping("/user/{userId}/is-new")
    @PreAuthorize("isAuthenticated()")
    public Map<String, Object> checkIfNewUser(@PathVariable Long userId, Authentication authentication) {
        requireSelfOrAdmin(userId, authentication);
        boolean isNew = coldStartOptimizationService.isNewUser(userId);
        return Map.of(
            "userId", userId,
            "isNewUser", isNew
        );
    }

    private void requireSelfOrAdmin(Long userId, Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        User currentUser = userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("User not found"));
        if (currentUser.getRole() != User.Role.ADMIN && !currentUser.getId().equals(userId)) {
            throw new AccessDeniedException("Cannot access another user's recommendation data");
        }
    }
}
