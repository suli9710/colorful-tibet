package com.tibet.tourism.service;

import com.tibet.tourism.dto.PriceInfo;
import com.tibet.tourism.entity.ScenicSpot;
import com.tibet.tourism.repository.ScenicSpotRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 价格更新服务
 */
@Service
public class PriceUpdateService {

    private static final Logger logger = LoggerFactory.getLogger(PriceUpdateService.class);
    private static final int BATCH_SIZE = 10;

    @Autowired
    private ScenicSpotRepository scenicSpotRepository;

    @Autowired
    private PriceFetchService priceFetchService;

    @Autowired
    @Qualifier("priceUpdateExecutor")
    private Executor priceUpdateExecutor;

    /**
     * 更新单个景点的价格
     */
    @Transactional
    public PriceUpdateResult updateSpotPrice(Long spotId, boolean forceUpdate) {
        ScenicSpot spot = scenicSpotRepository.findById(Long.valueOf(spotId))
            .orElseThrow(() -> new RuntimeException("景点不存在"));

        if (!forceUpdate && spot.getTicketPrice() != null &&
            spot.getTicketPrice().compareTo(BigDecimal.ZERO) > 0) {
            return new PriceUpdateResult(false, "景点已有价格，跳过更新", null);
        }

        try {
            PriceInfo priceInfo = priceFetchService.fetchPrice(spot);

            if (priceInfo == null || priceInfo.getBasePrice() == null) {
                return new PriceUpdateResult(false, "未能获取到价格信息", null);
            }

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
     * 批量更新所有景点的价格（并行处理）
     */
    @Transactional
    public BatchUpdateResult batchUpdatePrices(boolean forceUpdate) {
        List<ScenicSpot> spots = scenicSpotRepository.findAll();
        int total = spots.size();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger skipCount = new AtomicInteger(0);

        // 分批并行处理，每批最多 BATCH_SIZE 个
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < spots.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, spots.size());
            List<ScenicSpot> batch = spots.subList(i, end);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (ScenicSpot spot : batch) {
                    try {
                        PriceUpdateResult result = updateSpotPrice(spot.getId(), forceUpdate);
                        if (result.isSuccess()) {
                            successCount.incrementAndGet();
                        } else if (result.getMessage().contains("跳过")) {
                            skipCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        logger.error("批量更新景点价格失败: spotId={}, error={}", spot.getId(), e.getMessage());
                    }
                }
            }, priceUpdateExecutor);
            futures.add(future);
        }

        // 等待所有批次完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return new BatchUpdateResult(successCount.get(), failCount.get(), skipCount.get(), total);
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

