package com.tibet.tourism.dto.specialty;

import java.time.LocalDateTime;

public record TravelAlertResponse(
        String level,
        String type,
        String title,
        String message,
        String action,
        Integer relatedDay,
        LocalDateTime expiresAt
) {
}
