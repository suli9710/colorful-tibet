package com.tibet.tourism.modules.route.infra;
import com.tibet.tourism.modules.route.domain.ItineraryItem;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItineraryItemRepository extends JpaRepository<ItineraryItem, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT item FROM ItineraryItem item
            JOIN FETCH item.day day
            JOIN FETCH day.itinerary itinerary
            WHERE item.id = :id
            """)
    Optional<ItineraryItem> findByIdForUpdate(@Param("id") Long id);
}
