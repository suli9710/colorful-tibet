package com.tibet.tourism.modules.route.infra;
import com.tibet.tourism.modules.route.domain.TibetTravelKit;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TibetTravelKitRepository extends JpaRepository<TibetTravelKit, Long> {
    Optional<TibetTravelKit> findFirstByItineraryIdAndUserIdAndValidUntilAfterOrderByGeneratedAtDesc(
            Long itineraryId,
            Long userId,
            LocalDateTime now
    );

    void deleteByUserId(Long userId);

    void deleteByItineraryUserId(Long userId);
}
