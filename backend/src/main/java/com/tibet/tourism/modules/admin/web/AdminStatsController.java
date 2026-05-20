package com.tibet.tourism.modules.admin.web;
import com.tibet.tourism.modules.admin.application.AdminStatsService;
import com.tibet.tourism.modules.recommendation.application.ItemBasedRecommendationService;
import com.tibet.tourism.modules.recommendation.application.RecommendationEvaluationService;
import com.tibet.tourism.modules.recommendation.web.dto.RecommendationEvaluationResponse;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminStatsController {

    private final AdminStatsService adminStatsService;
    private final ItemBasedRecommendationService itemBasedRecommendationService;
    private final RecommendationEvaluationService recommendationEvaluationService;

    public AdminStatsController(AdminStatsService adminStatsService,
                                ItemBasedRecommendationService itemBasedRecommendationService,
                                RecommendationEvaluationService recommendationEvaluationService) {
        this.adminStatsService = adminStatsService;
        this.itemBasedRecommendationService = itemBasedRecommendationService;
        this.recommendationEvaluationService = recommendationEvaluationService;
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        return ResponseEntity.ok(adminStatsService.getDashboardStats());
    }

    @GetMapping("/recommendations/item-similarity/status")
    public ResponseEntity<Map<String, Object>> getItemSimilarityStatus() {
        return ResponseEntity.ok(itemBasedRecommendationService.getSimilarityMatrixStatus());
    }

    @PostMapping("/recommendations/item-similarity/precompute")
    public ResponseEntity<Map<String, Object>> precomputeItemSimilarity() {
        long startTime = System.currentTimeMillis();
        boolean started = itemBasedRecommendationService.precomputeItemSimilarityMatrix();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", started);
        body.put("message", started ? "景点相似度矩阵计算完成" : "景点相似度矩阵正在计算中");
        body.put("duration", System.currentTimeMillis() - startTime);
        body.put("status", itemBasedRecommendationService.getSimilarityMatrixStatus());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/recommendations/evaluation")
    public ResponseEntity<RecommendationEvaluationResponse> evaluateRecommendations(
            @RequestParam(required = false, defaultValue = "10") int k,
            @RequestParam(required = false, defaultValue = "1000") int userLimit,
            @RequestParam(required = false, defaultValue = "false") boolean includeUserResults) {
        return ResponseEntity.ok(recommendationEvaluationService.evaluateItemBasedCf(k, userLimit, includeUserResults));
    }
}
