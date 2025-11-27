package com.tibet.tourism.service;

import com.tibet.tourism.entity.ScenicSpot;
import com.tibet.tourism.entity.SpotTag;
import com.tibet.tourism.repository.ScenicSpotRepository;
import com.tibet.tourism.repository.SpotTagRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class SpotTagService {

    @Autowired
    private SpotTagRepository spotTagRepository;

    @Autowired
    private ScenicSpotRepository scenicSpotRepository;

    @Transactional(readOnly = true)
    public List<String> getTagsForSpot(@NonNull Long spotId) {
        ensureSpotExists(spotId);
        return spotTagRepository.findBySpotId(spotId).stream()
                .map(SpotTag::getTag)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<String> updateSpotTags(@NonNull Long spotId, List<String> tagNames) {
        ScenicSpot spot = scenicSpotRepository.findById(spotId)
                .orElseThrow(() -> new IllegalArgumentException("Scenic spot not found"));

        spotTagRepository.deleteBySpotId(spotId);

        List<String> sanitizedTags = sanitizeTags(tagNames);
        if (sanitizedTags.isEmpty()) {
            return Collections.emptyList();
        }

        List<SpotTag> newTags = sanitizedTags.stream()
                .map(tagValue -> {
                    SpotTag tag = new SpotTag();
                    tag.setSpot(spot);
                    tag.setTag(tagValue);
                    return tag;
                })
                .collect(Collectors.toList());
        spotTagRepository.saveAll(Objects.requireNonNull(newTags));
        return sanitizedTags;
    }

    private void ensureSpotExists(@NonNull Long spotId) {
        if (!scenicSpotRepository.existsById(spotId)) {
            throw new IllegalArgumentException("Scenic spot not found");
        }
    }

    private List<String> sanitizeTags(List<String> rawTags) {
        if (rawTags == null) {
            return Collections.emptyList();
        }

        Set<String> seen = new HashSet<>();
        List<String> sanitized = new ArrayList<>();
        for (String rawTag : rawTags) {
            String normalized = normalizeTag(rawTag);
            if (!StringUtils.hasText(normalized)) {
                continue;
            }
            String dedupKey = normalized.toLowerCase(Locale.ROOT);
            if (seen.add(dedupKey)) {
                sanitized.add(normalized);
            }
        }
        return sanitized;
    }

    private String normalizeTag(String value) {
        return value == null ? null : value.trim();
    }
}

