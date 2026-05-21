package com.tibet.tourism.modules.spot.web;
import com.tibet.tourism.common.error.ResourceNotFoundException;
import com.tibet.tourism.modules.spot.application.PriceFetchService;
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

    @Autowired
    private PriceFetchService priceFetchService;

    @Autowired
    private PriceUpdateService priceUpdateService;

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
            logger.warn("Price fetch requested for missing spot: spotId={}", spotId);
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Price fetch failed for spotId={}", spotId, e);
            return ResponseEntity.badRequest().body(Map.of("error", "价格查询失败，请稍后重试"));
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
            logger.warn("Price update requested for missing spot: spotId={}", spotId);
            return ResponseEntity.status(404).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Price update failed for spotId={}, force={}", spotId, force, e);
            return ResponseEntity.badRequest().body(Map.of("error", "价格查询失败，请稍后重试"));
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
            logger.error("Batch price update failed, force={}", force, e);
            return ResponseEntity.badRequest().body(Map.of("error", "价格查询失败，请稍后重试"));
        }
    }
}
