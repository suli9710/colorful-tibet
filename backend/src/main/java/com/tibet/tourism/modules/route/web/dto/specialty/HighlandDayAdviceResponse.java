package com.tibet.tourism.modules.route.web.dto.specialty;

public record HighlandDayAdviceResponse(
        Integer dayNumber,
        String title,
        Integer maxAltitudeMeters,
        String riskLevel,
        String paceAdvice,
        String hydrationAdvice,
        String activityLimit,
        String warning
) {
}
