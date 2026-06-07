package com.tibet.tourism.modules.spot.application;

import com.tibet.tourism.common.error.ResourceNotFoundException;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import com.tibet.tourism.modules.spot.web.dto.PriceInfo;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PriceUpdateService {

    private static final Logger logger = LoggerFactory.getLogger(PriceUpdateService.class);
    private static final int BATCH_SIZE = 10;
    private static final String SKIPPED_EXISTING_PRICE = "SKIPPED_EXISTING_PRICE";
    private static final String SKIPPED_REFERENCE_PRICE = "SKIPPED_REFERENCE_PRICE";

    @Value("${app.price-update.max-batch-spots:1000}")
    private int maxBatchSpots;

    @Autowired
    private ScenicSpotRepository scenicSpotRepository;

    @Autowired
    private PriceFetchService priceFetchService;

    @Autowired
    @Qualifier("priceUpdateExecutor")
    private Executor priceUpdateExecutor;

    @Transactional
    public PriceUpdateResult updateSpotPrice(Long spotId, boolean forceUpdate) {
        ScenicSpot spot = scenicSpotRepository.findById(spotId)
                .orElseThrow(() -> new ResourceNotFoundException("Scenic spot not found"));

        if (!forceUpdate && spot.getTicketPrice() != null
                && spot.getTicketPrice().compareTo(BigDecimal.ZERO) > 0) {
            return new PriceUpdateResult(false, SKIPPED_EXISTING_PRICE + ": scenic spot already has a price", null);
        }

        try {
            PriceInfo priceInfo = priceFetchService.fetchPrice(spot);
            if (priceInfo == null || priceInfo.getBasePrice() == null) {
                return new PriceUpdateResult(false, "NO_PRICE_INFO: price fetch returned no base price", null);
            }

            if (!priceFetchService.isPublishablePrice(priceInfo)) {
                return new PriceUpdateResult(
                        false,
                        SKIPPED_REFERENCE_PRICE + ": fetched price is reference-only or below publish confidence",
                        priceInfo);
            }

            spot.setTicketPrice(priceInfo.getBasePrice());
            if (priceInfo.getPeakSeasonPrice() != null) {
                spot.setPeakSeasonPrice(priceInfo.getPeakSeasonPrice());
            }
            if (priceInfo.getOffSeasonPrice() != null) {
                spot.setOffSeasonPrice(priceInfo.getOffSeasonPrice());
            }

            scenicSpotRepository.save(spot);
            return new PriceUpdateResult(true, "PRICE_UPDATED", priceInfo);
        } catch (Exception e) {
            return new PriceUpdateResult(false, "PRICE_UPDATE_FAILED: " + e.getMessage(), null);
        }
    }

    @Transactional
    public BatchUpdateResult batchUpdatePrices(boolean forceUpdate) {
        List<ScenicSpot> spots = scenicSpotRepository.findAllWithoutTags(
                PageRequest.of(0, Math.max(1, maxBatchSpots), Sort.by("id"))).getContent();
        int total = spots.size();
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger skipCount = new AtomicInteger(0);

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
                        } else if (isSkipMessage(result.getMessage())) {
                            skipCount.incrementAndGet();
                        } else {
                            failCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        failCount.incrementAndGet();
                        logger.error("Batch price update failed: spotId={}, error={}", spot.getId(), e.getMessage());
                    }
                }
            }, priceUpdateExecutor);
            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        return new BatchUpdateResult(successCount.get(), failCount.get(), skipCount.get(), total);
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
