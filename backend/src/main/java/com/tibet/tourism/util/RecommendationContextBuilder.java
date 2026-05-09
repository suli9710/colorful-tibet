package com.tibet.tourism.util;

import com.tibet.tourism.dto.RecommendationContext;

import java.util.Objects;
import java.util.stream.Stream;

public final class RecommendationContextBuilder {

    private RecommendationContextBuilder() {}

    public static RecommendationContext buildFromParams(
            String season, String weather, String currentLocation,
            Double currentLatitude, Double currentLongitude,
            String timeOfDay, String companion, Integer budget,
            Integer travelDays, String preferredActivities,
            Boolean considerDistance, Boolean considerBudget) {

        boolean hasAnyParam = Stream.of(season, weather, currentLocation,
                currentLatitude, currentLongitude, timeOfDay, companion,
                budget, travelDays, preferredActivities)
                .anyMatch(Objects::nonNull);

        if (!hasAnyParam) return null;

        RecommendationContext context = new RecommendationContext();
        context.setSeason(season);
        context.setWeather(weather);
        context.setCurrentLocation(currentLocation);
        context.setCurrentLatitude(currentLatitude);
        context.setCurrentLongitude(currentLongitude);
        context.setTimeOfDay(timeOfDay);
        context.setCompanion(companion);
        context.setBudget(budget);
        context.setTravelDays(travelDays);
        context.setPreferredActivities(preferredActivities);
        context.setConsiderDistance(considerDistance);
        context.setConsiderBudget(considerBudget);
        return context;
    }
}
