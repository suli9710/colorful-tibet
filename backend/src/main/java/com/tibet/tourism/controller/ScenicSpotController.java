package com.tibet.tourism.controller;

import com.tibet.tourism.dto.RecommendationDebugResponse;
import com.tibet.tourism.entity.ScenicSpot;
import com.tibet.tourism.service.RecommendationService;
import com.tibet.tourism.service.ScenicSpotService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/spots")
@CrossOrigin(origins = "*")
public class ScenicSpotController {

    @Autowired
    private ScenicSpotService scenicSpotService;

    @Autowired
    private RecommendationService recommendationService;

    @GetMapping
    public List<ScenicSpot> getAllSpots(@RequestParam(required = false) String category) {
        if (category != null) {
            return scenicSpotService.getSpotsByCategory(ScenicSpot.Category.valueOf(category.toUpperCase()));
        }
        return scenicSpotService.getAllSpots();
    }

    @GetMapping("/{id}")
    public ScenicSpot getSpotById(@PathVariable Long id) {
        return scenicSpotService.getSpotById(id);
    }

    @GetMapping("/search")
    public List<ScenicSpot> searchSpots(@RequestParam String keyword) {
        return scenicSpotService.searchSpots(keyword);
    }

    @GetMapping("/recommendations")
    public List<ScenicSpot> getRecommendations(@RequestParam Long userId) {
        return recommendationService.recommendSpotsForUser(userId);
    }

    @GetMapping("/recommendations/debug")
    public RecommendationDebugResponse getRecommendationDebug(@RequestParam Long userId) {
        return recommendationService.recommendWithDebug(userId);
    }
}
