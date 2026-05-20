package com.tibet.tourism.modules.content.web;
import com.tibet.tourism.modules.content.domain.Carousel;
import com.tibet.tourism.modules.content.infra.CarouselRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/carousels")
public class CarouselController {

    @Autowired
    private CarouselRepository carouselRepository;

    @GetMapping
    public ResponseEntity<List<Carousel>> getActiveCarousels() {
        return ResponseEntity.ok(carouselRepository.findByActiveTrueOrderBySortOrderAsc());
    }
}
