package com.tibet.tourism.modules.route.web.dto.itinerary;

import java.time.LocalDateTime;
import java.util.List;

/** Decision support, not medical advice, live availability or an execution guarantee. */
public record ItineraryReadinessResponse(
        String ruleVersion, java.time.OffsetDateTime evaluatedAt, String status,
        List<Issue> issues, List<Source> sources, List<ChecklistItem> checklist, String disclaimer) {
    public record Issue(String code, String severity, Integer dayNumber, Long itemId, String message) {}
    public record Source(String id, String title, String url, String status, LocalDateTime lastVerifiedAt) {}
    public record ChecklistItem(String id, String title, String status, String sourceId) {}
}
