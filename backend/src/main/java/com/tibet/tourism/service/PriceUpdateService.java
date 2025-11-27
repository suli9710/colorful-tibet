package com.tibet.tourism.service;

import com.tibet.tourism.dto.PriceInfo;
import com.tibet.tourism.entity.ScenicSpot;
import com.tibet.tourism.repository.ScenicSpotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 价格更新服务
 */
@Service
public class PriceUpdateService {

    @Autowired
    private ScenicSpotRepository scenicSpotRepository;

    @Autowired
    private PriceFetchService priceFetchService;

    /**
     * 更新单个景点的价格
     */
    @Transactional
    public PriceUpdateResult updateSpotPrice(Long spotId, boolean forceUpdate) {
        ScenicSpot spot = scenicSpotRepository.findById(Long.valueOf(spotId))
            .orElseThrow(() -> new RuntimeException("景点不存在"));

        // 如果已有价格且不强制更新，跳过
        if (!forceUpdate && spot.getTicketPrice() != null && 
            spot.getTicketPrice().compareTo(BigDecimal.ZERO) > 0) {
            return new PriceUpdateResult(false, "景点已有价格，跳过更新", null);
        }

        try {
            PriceInfo priceInfo = priceFetchService.fetchPrice(spot);
            
            if (priceInfo == null || priceInfo.getBasePrice() == null) {
                return new PriceUpdateResult(false, "未能获取到价格信息", null);
            }

            // 更新价格
            spot.setTicketPrice(priceInfo.getBasePrice());
            if (priceInfo.getPeakSeasonPrice() != null) {
                spot.setPeakSeasonPrice(priceInfo.getPeakSeasonPrice());
            }
            if (priceInfo.getOffSeasonPrice() != null) {
                spot.setOffSeasonPrice(priceInfo.getOffSeasonPrice());
            }

            scenicSpotRepository.save(spot);

            return new PriceUpdateResult(true, "价格更新成功", priceInfo);

        } catch (Exception e) {
            return new PriceUpdateResult(false, "价格更新失败: " + e.getMessage(), null);
        }
    }

    /**
     * 批量更新所有景点的价格
     */
    @Transactional
    public BatchUpdateResult batchUpdatePrices(boolean forceUpdate) {
        List<ScenicSpot> spots = scenicSpotRepository.findAll();
        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;

        for (ScenicSpot spot : spots) {
            PriceUpdateResult result = updateSpotPrice(spot.getId(), forceUpdate);
            if (result.isSuccess()) {
                successCount++;
            } else if (result.getMessage().contains("跳过")) {
                skipCount++;
            } else {
                failCount++;
            }
        }

        return new BatchUpdateResult(successCount, failCount, skipCount, spots.size());
    }

    /**
     * 价格更新结果
     */
    public static class PriceUpdateResult {
        private boolean success;
        private String message;
        private PriceInfo priceInfo;

        public PriceUpdateResult(boolean success, String message, PriceInfo priceInfo) {
            this.success = success;
            this.message = message;
            this.priceInfo = priceInfo;
        }

        public boolean isSuccess() {
            return success;
        }

        public String getMessage() {
            return message;
        }

        public PriceInfo getPriceInfo() {
            return priceInfo;
        }
    }

    /**
     * 批量更新结果
     */
    public static class BatchUpdateResult {
        private int successCount;
        private int failCount;
        private int skipCount;
        private int totalCount;

        public BatchUpdateResult(int successCount, int failCount, int skipCount, int totalCount) {
            this.successCount = successCount;
            this.failCount = failCount;
            this.skipCount = skipCount;
            this.totalCount = totalCount;
        }

        public int getSuccessCount() {
            return successCount;
        }

        public int getFailCount() {
            return failCount;
        }

        public int getSkipCount() {
            return skipCount;
        }

        public int getTotalCount() {
            return totalCount;
        }
    }
}

