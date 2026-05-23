package com.tibet.tourism.modules.route.infra;
import com.tibet.tourism.modules.route.domain.Itinerary;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {
    List<Itinerary> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Itinerary> findByIdAndUserId(Long id, Long userId);

    @Modifying
    @Query("""
            UPDATE Itinerary i
            SET i.parentItinerary = null
            WHERE i.parentItinerary.id IN (
                SELECT parent.id FROM Itinerary parent WHERE parent.user.id = :userId
            )
            """)
    void clearParentReferencesToUserItineraries(@Param("userId") Long userId);

    void deleteByUserId(Long userId);
}
