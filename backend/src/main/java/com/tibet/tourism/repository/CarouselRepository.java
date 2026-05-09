package com.tibet.tourism.repository;

import com.tibet.tourism.entity.Carousel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CarouselRepository extends JpaRepository<Carousel, Long> {
    List<Carousel> findByActiveTrueOrderBySortOrderAsc();
}
