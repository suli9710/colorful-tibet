package com.tibet.tourism.modules.route.web.dto.specialty;
import java.math.BigDecimal;

public record OfflineMapPinResponse(
        String type,
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        Integer altitudeMeters,
        String note
) {
}
