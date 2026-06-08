package com.tibet.tourism.modules.content.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

public record TibetanTranslationResponse(
        String chineseText,
        String tibetanText,
        @JsonInclude(JsonInclude.Include.NON_NULL) String message
) {
    public static TibetanTranslationResponse translated(String chineseText, String tibetanText) {
        return new TibetanTranslationResponse(chineseText, tibetanText, null);
    }

    public static TibetanTranslationResponse notFound(String chineseText) {
        return new TibetanTranslationResponse(chineseText, null, "Translation not found");
    }
}
