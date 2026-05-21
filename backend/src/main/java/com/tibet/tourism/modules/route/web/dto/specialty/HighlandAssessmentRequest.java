package com.tibet.tourism.modules.route.web.dto.specialty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;

public record HighlandAssessmentRequest(
        Long itineraryId,
        @Min(1) @Max(120) Integer age,
        @Min(1) @Max(20) Integer travelers,
        @Pattern(regexp = "^(LOW|MEDIUM|HIGH|low|medium|high)?$")
        String fitnessLevel,
        Boolean hasCardioRespiratoryHistory,
        Boolean withChildren,
        Boolean withSeniors,
        @Min(1) @Max(30) Integer days,
        @Min(0) @Max(9000) Integer maxAltitudeMeters,
        @Pattern(regexp = "^(RELAXED|BALANCED|FAST|relaxed|balanced|fast)?$")
        String pace
) {
}
