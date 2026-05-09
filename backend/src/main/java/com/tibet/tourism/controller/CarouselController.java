package com.tibet.tourism.controller;

import com.tibet.tourism.entity.Carousel;
import com.tibet.tourism.repository.CarouselRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
