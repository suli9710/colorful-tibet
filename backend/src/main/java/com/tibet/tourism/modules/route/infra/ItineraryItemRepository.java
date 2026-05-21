package com.tibet.tourism.modules.route.infra;
import com.tibet.tourism.modules.route.domain.ItineraryItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ItineraryItemRepository extends JpaRepository<ItineraryItem, Long> {
}
