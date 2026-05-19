package com.tibet.tourism.dto.specialty;

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
