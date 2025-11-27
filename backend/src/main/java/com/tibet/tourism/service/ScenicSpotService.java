package com.tibet.tourism.service;

import com.tibet.tourism.entity.ScenicSpot;
import com.tibet.tourism.repository.ScenicSpotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class ScenicSpotService {

    @Autowired
    private ScenicSpotRepository scenicSpotRepository;

    @Transactional(readOnly = true)
    public List<ScenicSpot> getAllSpots() {
        return scenicSpotRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<ScenicSpot> getAllSpotsWithoutTags() {
        return scenicSpotRepository.findAllWithoutTags();
    }

    @Transactional(readOnly = true)
    public ScenicSpot getSpotById(Long id) {
        return scenicSpotRepository.findByIdWithTags(id).orElseThrow(() -> new RuntimeException("Spot not found"));
    }

    @Transactional(readOnly = true)
    public List<ScenicSpot> searchSpots(String keyword) {
        return scenicSpotRepository.findByNameContaining(keyword);
    }

    @Transactional(readOnly = true)
    public List<ScenicSpot> getSpotsByCategory(ScenicSpot.Category category) {
        return scenicSpotRepository.findByCategory(category);
    }
}
