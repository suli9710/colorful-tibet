package com.tibet.tourism.dto.specialty;

public record PhraseGuideItemResponse(
        String category,
        String chinese,
        String tibetan,
        String english,
        String pronunciation,
        String usage
) {
}
