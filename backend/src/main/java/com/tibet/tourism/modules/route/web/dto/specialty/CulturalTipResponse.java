package com.tibet.tourism.modules.route.web.dto.specialty;
import java.util.List;

public record CulturalTipResponse(
        String scene,
        String title,
        String context,
        List<String> doTips,
        List<String> avoidTips
) {
}
