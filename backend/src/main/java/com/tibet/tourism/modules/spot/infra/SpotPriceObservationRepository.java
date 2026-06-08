package com.tibet.tourism.modules.spot.infra;

import com.tibet.tourism.modules.spot.domain.SpotPriceObservation;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpotPriceObservationRepository extends JpaRepository<SpotPriceObservation, Long> {
    List<SpotPriceObservation> findBySpotIdOrderByObservedAtDesc(Long spotId);
}
