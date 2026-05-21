package com.tibet.tourism.modules.route.infra;
import com.tibet.tourism.modules.route.domain.Itinerary;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {
    List<Itinerary> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Itinerary> findByIdAndUserId(Long id, Long userId);
}
