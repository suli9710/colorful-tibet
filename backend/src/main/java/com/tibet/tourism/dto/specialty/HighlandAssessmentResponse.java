package com.tibet.tourism.dto.specialty;

import java.util.List;

public record HighlandAssessmentResponse(
        Integer riskScore,
        String riskLevel,
        String riskLabel,
        Integer maxAltitudeMeters,
        Integer highAltitudeDays,
        String summary,
        List<HighlandDayAdviceResponse> dailyAdvice,
        List<String> adaptationChecklist,
        List<String> warningSigns,
        List<String> goSlowRules
) {
}
