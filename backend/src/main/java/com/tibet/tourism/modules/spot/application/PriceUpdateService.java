package com.tibet.tourism.modules.spot.application;

import com.tibet.tourism.common.error.ResourceNotFoundException;
import com.tibet.tourism.common.security.SensitiveLogSanitizer;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import com.tibet.tourism.modules.spot.web.dto.PriceInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

@Service
public class PriceUpdateService {

    private static final Logger logger = LoggerFactory.getLogger(PriceUpdateService.class);
    private static final int BATCH_SIZE = 10;

    @Value("${app.price-update.max-batch-spots:1000}")
    private int maxBatchSpots;

    private final ScenicSpotRepository scenicSpotRepository;
    private final SingleSpotPriceUpdateService singleSpotPriceUpdateService;
    private final Executor priceUpdateExecutor;

    public PriceUpdateService(
            ScenicSpotRepository scenicSpotRepository,
            SingleSpotPriceUpdateService singleSpotPriceUpdateService,
            @Qualifier("priceUpdateExecutor") Executor priceUpdateExecutor) {
        this.scenicSpotRepository = scenicSpotRepository;
        this.singleSpotPriceUpdateService = singleSpotPriceUpdateService;
        this.priceUpdateExecutor = priceUpdateExecutor;
    }

    public PriceUpdateResult updateSpotPrice(Long spotId, boolean forceUpdate) {
        return updateSpotPriceSafely(spotId, forceUpdate, true);
    }

    public BatchUpdateResult batchUpdatePrices(boolean forceUpdate) {
        List<Long> spotIds = scenicSpotRepository.findAllWithoutTags(
                        PageRequest.of(0, Math.max(1, maxBatchSpots), Sort.by("id")))
                .getContent()
                .stream()
                .map(ScenicSpot::getId)
                .toList();
        int total = spotIds.size();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger skipCount = new AtomicInteger(0);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (int i = 0; i < spotIds.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, spotIds.size());
            List<Long> batch = spotIds.subList(i, end);

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                for (Long spotId : batch) {
                    try {
                        PriceUpdateResult result = updateSpotPriceSafely(spotId, forceUpdate, false);
                        if (result.isSuccess()) {
                            successCount.incrementAndGet();
                        } else if (isSkipMessage(result.getMessage())) {
                            skipCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        logger.error("Batch price update failed: spotId={}, detail={}",
                                spotId, SensitiveLogSanitizer.exceptionSummary(e));
                    }
                }
            }, priceUpdateExecutor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return new BatchUpdateResult(successCount.get(), failCount.get(), skipCount.get(), total);
    }

    private PriceUpdateResult updateSpotPriceSafely(Long spotId, boolean forceUpdate, boolean rethrowNotFound) {
        try {
            return singleSpotPriceUpdateService.updateSpotPrice(spotId, forceUpdate);
        } catch (ResourceNotFoundException e) {
            if (rethrowNotFound) {
                throw e;
            }
            logger.warn("Price update failed: spotId={}, detail={}",
                    spotId, SensitiveLogSanitizer.exceptionSummary(e));
            return new PriceUpdateResult(false, "PRICE_UPDATE_FAILED", null);
        } catch (Exception e) {
            logger.warn("Price update failed: spotId={}, detail={}",
                    spotId, SensitiveLogSanitizer.exceptionSummary(e));
            return new PriceUpdateResult(false, "PRICE_UPDATE_FAILED", null);
        }
    }

    private boolean isSkipMessage(String message) {
        return message != null && (message.contains("SKIPPED_") || message.toLowerCase().contains("skip"));
    }

    public static class PriceUpdateResult {
        private final boolean success;
        private final String message;
        private final PriceInfo priceInfo;

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

    public static class BatchUpdateResult {
        private final int successCount;
        private final int failCount;
        private final int skipCount;
        private final int totalCount;

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
