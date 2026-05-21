package com.tibet.tourism.modules.content.infra;
import com.tibet.tourism.modules.content.domain.Carousel;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CarouselRepository extends JpaRepository<Carousel, Long> {
    List<Carousel> findByActiveTrueOrderBySortOrderAsc();
}
