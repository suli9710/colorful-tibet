package com.tibet.tourism.repository;

import com.tibet.tourism.entity.Itinerary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItineraryRepository extends JpaRepository<Itinerary, Long> {
    List<Itinerary> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Itinerary> findByIdAndUserId(Long id, Long userId);
}
