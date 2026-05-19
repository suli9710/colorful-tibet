package com.tibet.tourism.repository;

import com.tibet.tourism.entity.TibetTravelKit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TibetTravelKitRepository extends JpaRepository<TibetTravelKit, Long> {
    Optional<TibetTravelKit> findFirstByItineraryIdAndUserIdAndValidUntilAfterOrderByGeneratedAtDesc(
            Long itineraryId,
            Long userId,
            LocalDateTime now
    );
}
