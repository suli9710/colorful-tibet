package com.tibet.tourism.modules.content.web.dto;

import com.tibet.tourism.modules.content.domain.TibetanDictionary;
import java.time.LocalDateTime;

public record TibetanDictionaryResponse(
        Long id,
        String chineseText,
        String tibetanText,
        TibetanDictionary.Type type,
        Integer usageCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TibetanDictionaryResponse from(TibetanDictionary entry) {
        return new TibetanDictionaryResponse(
                entry.getId(),
                entry.getChineseText(),
                entry.getTibetanText(),
                entry.getType(),
                entry.getUsageCount(),
                entry.getCreatedAt(),
                entry.getUpdatedAt());
    }
}
