package com.tibet.tourism.dto.specialty;

import java.util.List;

public record TibetTravelKitResponse(
        Long itineraryId,
        String title,
        HighlandAssessmentResponse highlandAssessment,
        List<CulturalTipResponse> culturalTips,
        List<PhraseGuideItemResponse> phrasebook,
        OfflineTravelPackageResponse offlinePackage,
        List<TravelAlertResponse> realtimeAlerts,
        List<SustainableOptionResponse> sustainableOptions
) {
}
