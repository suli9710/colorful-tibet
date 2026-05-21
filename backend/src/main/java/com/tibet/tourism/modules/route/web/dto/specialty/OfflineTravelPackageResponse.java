package com.tibet.tourism.modules.route.web.dto.specialty;
import java.time.LocalDateTime;
import java.util.List;

public record OfflineTravelPackageResponse(
        Long itineraryId,
        String title,
        LocalDateTime generatedAt,
        LocalDateTime validUntil,
        List<String> includedSections,
        List<EmergencyContactResponse> emergencyContacts,
        List<OfflineMapPinResponse> mapPins,
        List<String> offlineChecklist,
        List<String> voucherHints,
        List<String> backupNotes
) {
}
