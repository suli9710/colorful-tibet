package com.tibet.tourism.modules.spot.application;

import com.tibet.tourism.common.error.ResourceNotFoundException;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import com.tibet.tourism.modules.spot.web.dto.PriceInfo;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;

@Service
public class SingleSpotPriceUpdateService {

    static final String SKIPPED_EXISTING_PRICE = "SKIPPED_EXISTING_PRICE";
    static final String SKIPPED_REFERENCE_PRICE = "SKIPPED_REFERENCE_PRICE";

    private final ScenicSpotRepository scenicSpotRepository;
    private final PriceFetchService priceFetchService;
    private final PriceUpdatePersistenceService priceUpdatePersistenceService;

    public SingleSpotPriceUpdateService(
            ScenicSpotRepository scenicSpotRepository,
            PriceFetchService priceFetchService,
            PriceUpdatePersistenceService priceUpdatePersistenceService) {
        this.scenicSpotRepository = scenicSpotRepository;
        this.priceFetchService = priceFetchService;
        this.priceUpdatePersistenceService = priceUpdatePersistenceService;
    }

    public PriceUpdateService.PriceUpdateResult updateSpotPrice(Long spotId, boolean forceUpdate) {
        // Repository reads complete before the external provider call. Persistence happens in a
        // separate short transaction so a slow provider never holds a JDBC connection or row lock.
        ScenicSpot spot = scenicSpotRepository.findById(spotId)
                .orElseThrow(() -> new ResourceNotFoundException("Scenic spot not found"));

        if (!forceUpdate && spot.getTicketPrice() != null
                && spot.getTicketPrice().compareTo(BigDecimal.ZERO) > 0) {
            return new PriceUpdateService.PriceUpdateResult(
                    false,
                    SKIPPED_EXISTING_PRICE + ": scenic spot already has a price",
                    null);
        }

        PriceInfo priceInfo = priceFetchService.fetchPrice(spot);
        if (priceInfo == null || priceInfo.getBasePrice() == null) {
            return new PriceUpdateService.PriceUpdateResult(
                    false,
                    "NO_PRICE_INFO: price fetch returned no base price",
                    null);
        }

        boolean publishablePrice = priceFetchService.isPublishablePrice(priceInfo);
        if (!publishablePrice) {
            priceUpdatePersistenceService.saveReviewObservation(spotId, priceInfo);
            return new PriceUpdateService.PriceUpdateResult(
                    false,
                    SKIPPED_REFERENCE_PRICE + ": fetched price is reference-only or below publish confidence",
                    priceInfo);
        }

        return priceUpdatePersistenceService.publishFetchedPrice(spotId, forceUpdate, priceInfo);
    }
}
