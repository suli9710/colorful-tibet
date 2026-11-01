package com.tibet.tourism.modules.route.web.dto.itinerary;

import jakarta.validation.constraints.*;

/** Only timing is editable here; price, supplier, identity and booking state are server-owned. */
public record UpdateItineraryItemRequest(
        @NotBlank @Pattern(regexp = "^(?:[01][0-9]|2[0-3]):[0-5][0-9]$") String startTime,
        @NotNull @Min(1) @Max(1440) Integer durationMinutes,
        @NotNull @Min(0) Long expectedVersion) {}
