package com.tibet.tourism.modules.spot.web;
import com.tibet.tourism.common.api.PageResponse;
import com.tibet.tourism.common.security.SensitiveLogSanitizer;
import com.tibet.tourism.common.validation.InputSanitizer;
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
import com.tibet.tourism.modules.spot.web.dto.ScenicSpotResponse;
import com.tibet.tourism.modules.spot.web.dto.UserPreferenceDTO;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/spots")
public class ScenicSpotController {

    private static final Logger logger = LoggerFactory.getLogger(ScenicSpotController.class);
    private static final Set<String> ALLOWED_SPOT_SORT_FIELDS = Set.of(
            "id", "name", "category", "ticketPrice", "rating", "visitCount", "createdAt");
    private static final Sort DEFAULT_SPOT_SORT = Sort.by("id");
    private static final int MAX_SIMILAR_SPOTS_LIMIT = 20;

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
    public PageResponse<ScenicSpotResponse> getAllSpots(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "zh") String locale,
            @PageableDefault(size = 20) Pageable pageable) {
        Pageable safePageable = InputSanitizer.sanitizePageable(
                pageable, ALLOWED_SPOT_SORT_FIELDS, DEFAULT_SPOT_SORT, 20, 100);
        Page<ScenicSpot> spots;
        if (category != null) {
            spots = scenicSpotService.getSpotsByCategory(ScenicSpot.Category.valueOf(category.toUpperCase()), safePageable);
        } else {
            spots = scenicSpotService.getAllSpots(safePageable);
        }

        return PageResponse.from(spots.map(spot -> ScenicSpotResponse.fromEntity(spot, locale)));
    }

    @GetMapping("/{id}")
    public ScenicSpotResponse getSpotById(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "zh") String locale,
            Authentication authentication) {
        ScenicSpot spot = scenicSpotService.getSpotById(id);
        recordAuthenticatedSpotView(authentication, spot);
        return ScenicSpotResponse.fromEntity(spot, locale);
    }

    @GetMapping("/heatmap")
    public List<ScenicSpotHeatmapPointDTO> getHeatmapSpots(
            @RequestParam(required = false, defaultValue = "zh") String locale,
            @RequestParam(required = false, defaultValue = "100") int limit) {
        return scenicSpotService.getHeatmapSpots(locale, limit);
    }

    @GetMapping("/search")
    public PageResponse<ScenicSpotResponse> searchSpots(
            @RequestParam String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "zh") String locale,
            @PageableDefault(size = 20) Pageable pageable) {
        Pageable safePageable = InputSanitizer.sanitizePageable(
                pageable, ALLOWED_SPOT_SORT_FIELDS, DEFAULT_SPOT_SORT, 20, 100);
        Page<ScenicSpot> spots = scenicSpotService.searchSpots(
                InputSanitizer.requiredPlainText(keyword, 100, "搜索关键词"),
                parseCategoryOrNull(category),
                safePageable);
        return PageResponse.from(spots.map(spot -> ScenicSpotResponse.fromEntity(spot, locale)));
    }

    private ScenicSpot.Category parseCategoryOrNull(String category) {
        String normalized = InputSanitizer.optionalPlainText(category, 40, "景点类别");
        if (normalized == null) {
            return null;
        }
        try {
            return ScenicSpot.Category.valueOf(normalized.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid spot category");
        }
    }

    @GetMapping("/recommendations")
    @PreAuthorize("isAuthenticated()")
    public List<ScenicSpotResponse> getRecommendations(
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
        
        return recommendForUser(userId, context, locale);
    }

    @GetMapping("/recommendations/me")
    @PreAuthorize("isAuthenticated()")
    public List<ScenicSpotResponse> getMyRecommendations(
            @RequestParam(required = false, defaultValue = "zh") String locale,
            // 涓婁笅鏂囧弬鏁帮紙鍙€夛級
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
        Long currentUserId = currentUserId(authentication);
        RecommendationContext context = RecommendationContextBuilder.buildFromParams(
                season, weather, currentLocation, currentLatitude, currentLongitude,
                timeOfDay, companion, budget, travelDays, preferredActivities,
                considerDistance, considerBudget);

        return recommendForUser(currentUserId, context, locale);
    }
    
    @PostMapping("/recommendations")
    @PreAuthorize("isAuthenticated()")
    public List<ScenicSpotResponse> getRecommendationsWithContext(
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "zh") String locale,
            @RequestBody(required = false) RecommendationContext context,
            Authentication authentication) {
        requireSelfOrAdmin(userId, authentication);
        
        return recommendForUser(userId, context, locale);
    }

    @PostMapping("/recommendations/me")
    @PreAuthorize("isAuthenticated()")
    public List<ScenicSpotResponse> getMyRecommendationsWithContext(
            @RequestParam(required = false, defaultValue = "zh") String locale,
            @RequestBody(required = false) RecommendationContext context,
            Authentication authentication) {
        return recommendForUser(currentUserId(authentication), context, locale);
    }

    @GetMapping("/recommendations/debug")
    @PreAuthorize("hasRole('ADMIN')")
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
        
        return recommendationService.recommendWithDebug(userId, context, locale);
    }

    @PostMapping("/recommendations/debug")
    @PreAuthorize("hasRole('ADMIN')")
    public RecommendationDebugResponse getRecommendationDebugWithContext(
            @RequestParam Long userId,
            @RequestParam(required = false, defaultValue = "zh") String locale,
            @RequestBody(required = false) RecommendationContext context,
            Authentication authentication) {
        requireSelfOrAdmin(userId, authentication);

        return recommendationService.recommendWithDebug(userId, context, locale);
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
                "message", "Scenic spot similarity matrix computed",
                "duration", endTime - startTime
            );
        } catch (Exception e) {
            logger.error("Failed to precompute scenic spot similarity matrix: {}", SensitiveLogSanitizer.exceptionSummary(e));
            return Map.of(
                "success", false,
                "message", "Calculation failed; please retry later"
            );
        }
    }
    
    /**
     * 获取与指定景点相似的景点
     */
    @GetMapping("/{id}/similar")
    public Map<String, Object> getSimilarSpots(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "10") int limit,
            @RequestParam(required = false, defaultValue = "zh") String locale) {
        int safeLimit = Math.min(Math.max(limit, 1), MAX_SIMILAR_SPOTS_LIMIT);
        Map<Long, Double> similarSpots = itemBasedRecommendationService.getSimilarSpots(id, safeLimit);
        List<ScenicSpotResponse> spots = scenicSpotService
                .getSpotsByIdsPreservingOrder(similarSpots.keySet().stream().toList()).stream()
                .map(spot -> ScenicSpotResponse.fromEntity(spot, locale))
                .toList();
        return Map.of(
            "spotId", id,
            "spots", spots,
            "count", spots.size()
        );
    }
    
    /**
     * 新用户冷启动推荐
     */
    @PostMapping("/recommendations/cold-start")
    @PreAuthorize("isAuthenticated()")
    public List<ScenicSpotResponse> getColdStartRecommendations(
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
        
        return recommendations.stream()
                .map(spot -> ScenicSpotResponse.fromEntity(spot, locale))
                .toList();
    }

    /**
     * 基于位置的冷启动推荐
     */
    @GetMapping("/recommendations/cold-start/location")
    @PreAuthorize("isAuthenticated()")
    public List<ScenicSpotResponse> getColdStartRecommendationsByLocation(
            @RequestParam Long userId,
            @RequestParam Double latitude,
            @RequestParam Double longitude,
            @RequestParam(required = false, defaultValue = "50.0") Double maxDistanceKm,
            @RequestParam(required = false, defaultValue = "zh") String locale,
            Authentication authentication) {
        requireSelfOrAdmin(userId, authentication);

        List<ScenicSpot> recommendations = coldStartOptimizationService
                .recommendForNewUserByLocation(latitude, longitude, maxDistanceKm);
        return recommendations.stream()
                .map(spot -> ScenicSpotResponse.fromEntity(spot, locale))
                .toList();
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
            "isNewUser", isNew
        );
    }

    private void requireSelfOrAdmin(Long userId, Authentication authentication) {
        User currentUser = currentUser(authentication);
        if (currentUser.getRole() != User.Role.ADMIN && !currentUser.getId().equals(userId)) {
            throw new AccessDeniedException("Cannot access another user's recommendation data");
        }
    }

    private Long currentUserId(Authentication authentication) {
        return currentUser(authentication).getId();
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AccessDeniedException("Authentication required");
        }
        return userRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new AccessDeniedException("User not found"));
    }

    private void recordAuthenticatedSpotView(Authentication authentication, ScenicSpot spot) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            return;
        }
        try {
            Optional<User> user = userRepository.findByUsername(authentication.getName());
            user.ifPresent(value -> recommendationService.recordSpotView(value, spot));
        } catch (Exception e) {
            logger.warn("Failed to record scenic spot view: detail={}", SensitiveLogSanitizer.exceptionSummary(e));
        }
    }

    private List<ScenicSpotResponse> recommendForUser(Long userId, RecommendationContext context, String locale) {
        List<ScenicSpot> spots = recommendationService.recommendSpotsForUser(userId, context);
        return spots.stream()
                .map(spot -> ScenicSpotResponse.fromEntity(spot, locale))
                .toList();
    }
}
