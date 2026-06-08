package com.tibet.tourism.modules.spot.web;
import com.tibet.tourism.common.error.ResourceNotFoundException;
import com.tibet.tourism.common.security.SensitiveLogSanitizer;
import com.tibet.tourism.modules.spot.application.PriceFetchService;
import com.tibet.tourism.modules.spot.application.PriceBatchUpdateJobService;
import com.tibet.tourism.modules.spot.application.PriceUpdateService;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import com.tibet.tourism.modules.spot.web.dto.PriceInfo;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 价格管理Controller
 */
@RestController
@RequestMapping("/api/prices")
public class PriceController {

    private static final Logger logger = LoggerFactory.getLogger(PriceController.class);
    private static final String PRICE_NOT_FOUND_ERROR = "Price resource not found";
    private static final String PRICE_OPERATION_ERROR = "Price operation could not be processed";

    @Autowired
    private PriceFetchService priceFetchService;

    @Autowired
    private PriceUpdateService priceUpdateService;

    @Autowired
    private PriceBatchUpdateJobService priceBatchUpdateJobService;

    @Autowired
    private ScenicSpotRepository scenicSpotRepository;

    /**
     * 获取景点价格信息（不更新数据库，仅查询）
     */
    @GetMapping("/fetch/{spotId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> fetchPrice(@PathVariable Long spotId) {
        try {
            ScenicSpot spot = scenicSpotRepository.findById(Long.valueOf(spotId))
                .orElseThrow(() -> new ResourceNotFoundException("景点不存在"));

            PriceInfo priceInfo = priceFetchService.fetchPrice(spot);

            if (priceInfo == null) {
                return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "未能获取到价格信息"
                ));
            }

            return ResponseEntity.ok(Map.of(
                "success", true,
                "priceInfo", priceInfo,
                "currentPrice", spot.getTicketPrice()
            ));
        } catch (ResourceNotFoundException e) {
            logger.warn("Price fetch requested for missing spot: spotId={}, detail={}",
                    spotId, SensitiveLogSanitizer.exceptionSummary(e));
            return ResponseEntity.status(404).body(Map.of("error", PRICE_NOT_FOUND_ERROR));
        } catch (Exception e) {
            logger.error("Price fetch failed: spotId={}, detail={}",
                    spotId, SensitiveLogSanitizer.exceptionSummary(e));
            return ResponseEntity.badRequest().body(Map.of("error", PRICE_OPERATION_ERROR));
        }
    }

    /**
     * 更新单个景点价格
     */
    @PostMapping("/update/{spotId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updatePrice(
            @PathVariable Long spotId,
            @RequestParam(required = false, defaultValue = "false") boolean force) {
        try {
            PriceUpdateService.PriceUpdateResult result = priceUpdateService.updateSpotPrice(spotId, force);
            return ResponseEntity.ok(result);
        } catch (ResourceNotFoundException e) {
            logger.warn("Price update requested for missing spot: spotId={}, detail={}",
                    spotId, SensitiveLogSanitizer.exceptionSummary(e));
            return ResponseEntity.status(404).body(Map.of("error", PRICE_NOT_FOUND_ERROR));
        } catch (Exception e) {
            logger.error("Price update failed: spotId={}, force={}, detail={}",
                    spotId, force, SensitiveLogSanitizer.exceptionSummary(e));
            return ResponseEntity.badRequest().body(Map.of("error", PRICE_OPERATION_ERROR));
        }
    }

    /**
     * 批量更新所有景点价格
     */
    @PostMapping("/batch-update")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> batchUpdatePrices(
            @RequestParam(required = false, defaultValue = "false") boolean force) {
        try {
            PriceUpdateService.BatchUpdateResult result = priceUpdateService.batchUpdatePrices(force);
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            logger.error("Batch price update failed: force={}, detail={}",
                    force, SensitiveLogSanitizer.exceptionSummary(e));
            return ResponseEntity.badRequest().body(Map.of("error", PRICE_OPERATION_ERROR));
        }
    }

    @PostMapping("/batch-update/jobs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> startBatchUpdateJob(
            @RequestParam(required = false, defaultValue = "false") boolean force) {
        try {
            return ResponseEntity.ok(priceBatchUpdateJobService.startJob(force));
        } catch (Exception e) {
            logger.error("Batch price update job start failed: force={}, detail={}",
                    force, SensitiveLogSanitizer.exceptionSummary(e));
            return ResponseEntity.badRequest().body(Map.of("error", PRICE_OPERATION_ERROR));
        }
    }

    @GetMapping("/batch-update/jobs/{jobId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getBatchUpdateJob(@PathVariable String jobId) {
        try {
            return ResponseEntity.ok(priceBatchUpdateJobService.getJob(jobId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }
}
