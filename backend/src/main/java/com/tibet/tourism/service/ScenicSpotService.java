package com.tibet.tourism.service;

import com.tibet.tourism.dto.ScenicSpotHeatmapPointDTO;
import com.tibet.tourism.entity.ScenicSpot;
import com.tibet.tourism.repository.ScenicSpotRepository;
import com.tibet.tourism.util.LocaleHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    public Page<ScenicSpot> getAllSpots(Pageable pageable) {
        return scenicSpotRepository.findAllWithoutTags(pageable);
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
    public Page<ScenicSpot> searchSpots(String keyword, Pageable pageable) {
        return scenicSpotRepository.findByNameContaining(keyword, pageable);
    }

    @Transactional(readOnly = true)
    public List<ScenicSpot> getSpotsByCategory(ScenicSpot.Category category) {
        return scenicSpotRepository.findByCategory(category);
    }

    @Transactional(readOnly = true)
    public Page<ScenicSpot> getSpotsByCategory(ScenicSpot.Category category, Pageable pageable) {
        return scenicSpotRepository.findByCategory(category, pageable);
    }

    @Transactional(readOnly = true)
    public List<ScenicSpotHeatmapPointDTO> getHeatmapSpots(String locale, int limit) {
        int normalizedLimit = Math.min(Math.max(limit, 1), 500);
        return scenicSpotRepository.findHeatmapSpots(PageRequest.of(0, normalizedLimit)).stream()
                .map(spot -> {
                    String localizedName = LocaleHelper.resolveByLocale(locale, spot.getName(), spot.getNameTibetan());
                    return new ScenicSpotHeatmapPointDTO(
                            spot.getId(),
                            localizedName,
                            spot.getLongitude().doubleValue(),
                            spot.getLatitude().doubleValue(),
                            spot.getVisitCount() != null ? spot.getVisitCount() : 0
                    );
                })
                .toList();
    }
}
