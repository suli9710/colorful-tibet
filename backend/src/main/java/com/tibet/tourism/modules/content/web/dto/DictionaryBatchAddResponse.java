package com.tibet.tourism.modules.content.web.dto;

import java.util.List;

public record DictionaryBatchAddResponse(
        String message,
        int count,
        List<TibetanDictionaryResponse> entries
) {
}
