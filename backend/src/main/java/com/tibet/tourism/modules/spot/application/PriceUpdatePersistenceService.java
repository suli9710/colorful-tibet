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
public class PriceUpdatePersistenceService {

    private final ScenicSpotRepository scenicSpotRepository;
    private final SpotPriceObservationRepository priceObservationRepository;

    public PriceUpdatePersistenceService(
            ScenicSpotRepository scenicSpotRepository,
            SpotPriceObservationRepository priceObservationRepository) {
        this.scenicSpotRepository = scenicSpotRepository;
        this.priceObservationRepository = priceObservationRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PriceUpdateService.PriceUpdateResult publishFetchedPrice(
            Long spotId,
            boolean forceUpdate,
            PriceInfo priceInfo) {
        ScenicSpot spot = scenicSpotRepository.findByIdForUpdate(spotId)
                .orElseThrow(() -> new ResourceNotFoundException("Scenic spot not found"));

        // Re-check after the external call. An administrator may have supplied a price while the
        // provider request was in flight; non-forced background refreshes must not overwrite it.
        if (!forceUpdate && hasPublishedPrice(spot)) {
            return new PriceUpdateService.PriceUpdateResult(
                    false,
                    SingleSpotPriceUpdateService.SKIPPED_EXISTING_PRICE
                            + ": scenic spot received a price while fetch was in progress",
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
        saveObservation(
                spot,
                priceInfo,
                true,
                SpotPriceObservation.Status.PUBLISHED,
                "Published to scenic spot");
        return new PriceUpdateService.PriceUpdateResult(true, "PRICE_UPDATED", priceInfo);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void saveReviewObservation(Long spotId, PriceInfo priceInfo) {
        ScenicSpot spot = scenicSpotRepository.findById(spotId)
                .orElseThrow(() -> new ResourceNotFoundException("Scenic spot not found"));
        saveObservation(
                spot,
                priceInfo,
                false,
                SpotPriceObservation.Status.REVIEW_REQUIRED,
                "Reference-only or below publish confidence");
    }

    private boolean hasPublishedPrice(ScenicSpot spot) {
        return spot.getTicketPrice() != null && spot.getTicketPrice().compareTo(BigDecimal.ZERO) > 0;
    }

    private void saveObservation(
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
