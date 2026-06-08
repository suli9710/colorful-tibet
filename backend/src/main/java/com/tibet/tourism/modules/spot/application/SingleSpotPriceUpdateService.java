package com.tibet.tourism.modules.spot.application;

import com.tibet.tourism.common.error.ResourceNotFoundException;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.domain.SpotPriceObservation;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import com.tibet.tourism.modules.spot.infra.SpotPriceObservationRepository;
import com.tibet.tourism.modules.spot.web.dto.PriceInfo;
import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SingleSpotPriceUpdateService {

    static final String SKIPPED_EXISTING_PRICE = "SKIPPED_EXISTING_PRICE";
    static final String SKIPPED_REFERENCE_PRICE = "SKIPPED_REFERENCE_PRICE";

    private final ScenicSpotRepository scenicSpotRepository;
    private final PriceFetchService priceFetchService;
    private final SpotPriceObservationRepository priceObservationRepository;

    public SingleSpotPriceUpdateService(
            ScenicSpotRepository scenicSpotRepository,
            PriceFetchService priceFetchService,
            SpotPriceObservationRepository priceObservationRepository) {
        this.scenicSpotRepository = scenicSpotRepository;
        this.priceFetchService = priceFetchService;
        this.priceObservationRepository = priceObservationRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PriceUpdateService.PriceUpdateResult updateSpotPrice(Long spotId, boolean forceUpdate) {
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
            savePriceObservation(
                    spot,
                    priceInfo,
                    false,
                    SpotPriceObservation.Status.REVIEW_REQUIRED,
                    "Reference-only or below publish confidence");
            return new PriceUpdateService.PriceUpdateResult(
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
        savePriceObservation(spot, priceInfo, true, SpotPriceObservation.Status.PUBLISHED, "Published to scenic spot");
        return new PriceUpdateService.PriceUpdateResult(true, "PRICE_UPDATED", priceInfo);
    }

    private void savePriceObservation(
            ScenicSpot spot,
            PriceInfo priceInfo,
            boolean publishable,
            SpotPriceObservation.Status status,
            String reviewReason) {
        priceObservationRepository.save(SpotPriceObservation.from(
                spot,
                priceInfo,
                publishable,
                status,
                reviewReason));
    }
}
