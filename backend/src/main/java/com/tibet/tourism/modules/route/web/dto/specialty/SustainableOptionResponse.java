package com.tibet.tourism.modules.route.web.dto.specialty;
import java.util.List;

public record SustainableOptionResponse(
        String category,
        String title,
        String impact,
        List<String> actions,
        String localBenefit,
        String carbonHint
) {
}
