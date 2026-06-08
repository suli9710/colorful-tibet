package com.tibet.tourism.modules.spot.application;
import com.tibet.tourism.common.error.ResourceNotFoundException;
import com.tibet.tourism.common.util.LocaleHelper;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import com.tibet.tourism.modules.spot.web.dto.ScenicSpotHeatmapPointDTO;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ScenicSpotService {

    private static final int DEFAULT_LIST_LIMIT = 500;

    @Autowired
    private ScenicSpotRepository scenicSpotRepository;

    @Transactional(readOnly = true)
    public List<ScenicSpot> getAllSpots() {
        return scenicSpotRepository.findAllWithoutTags(PageRequest.of(0, DEFAULT_LIST_LIMIT, Sort.by("id"))).getContent();
    }

    @Transactional(readOnly = true)
    public Page<ScenicSpot> getAllSpots(Pageable pageable) {
        return scenicSpotRepository.findAllWithoutTags(pageable);
    }

    @Transactional(readOnly = true)
    public List<ScenicSpot> getAllSpotsWithoutTags() {
        return scenicSpotRepository.findAllWithoutTags(PageRequest.of(0, DEFAULT_LIST_LIMIT, Sort.by("id"))).getContent();
    }

    @Transactional(readOnly = true)
    public ScenicSpot getSpotById(Long id) {
        return scenicSpotRepository.findByIdWithTags(id).orElseThrow(() -> new ResourceNotFoundException("Spot not found"));
    }

    @Transactional(readOnly = true)
    public List<ScenicSpot> searchSpots(String keyword) {
        return scenicSpotRepository.findByNameContaining(keyword, PageRequest.of(0, DEFAULT_LIST_LIMIT, Sort.by("id"))).getContent();
    }

    @Transactional(readOnly = true)
    public Page<ScenicSpot> searchSpots(String keyword, Pageable pageable) {
        return scenicSpotRepository.findByNameContaining(keyword, pageable);
    }

    @Transactional(readOnly = true)
    public List<ScenicSpot> getSpotsByCategory(ScenicSpot.Category category) {
        return scenicSpotRepository.findByCategory(category, PageRequest.of(0, DEFAULT_LIST_LIMIT, Sort.by("id"))).getContent();
    }

    @Transactional(readOnly = true)
    public List<ScenicSpot> getSpotsByIdsPreservingOrder(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }

        List<Long> safeIds = ids.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (safeIds.isEmpty()) {
            return List.of();
        }

        Map<Long, ScenicSpot> spotsById = scenicSpotRepository.findByIdInWithTags(safeIds).stream()
                .filter(spot -> spot.getId() != null)
                .collect(Collectors.toMap(
                        ScenicSpot::getId,
                        spot -> spot,
                        (left, right) -> left));
        return safeIds.stream()
                .map(spotsById::get)
                .filter(Objects::nonNull)
                .toList();
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
