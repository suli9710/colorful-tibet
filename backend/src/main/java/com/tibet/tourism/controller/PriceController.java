package com.tibet.tourism.controller;

import com.tibet.tourism.dto.PriceInfo;
import com.tibet.tourism.entity.ScenicSpot;
import com.tibet.tourism.repository.ScenicSpotRepository;
import com.tibet.tourism.service.PriceFetchService;
import com.tibet.tourism.service.PriceUpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 价格管理Controller
 */
@RestController
@RequestMapping("/api/prices")
@CrossOrigin(origins = "*")
public class PriceController {

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
    public ResponseEntity<?> fetchPrice(@PathVariable Long spotId) {
        try {
            ScenicSpot spot = scenicSpotRepository.findById(Long.valueOf(spotId))
                .orElseThrow(() -> new RuntimeException("景点不存在"));
            
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
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}

