package com.tibet.tourism.modules.recommendation.application.strategy;
import com.tibet.tourism.modules.recommendation.web.dto.RecommendationContext;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.domain.SpotTag;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ContextAwarePostProcessor {

    private static final Logger logger = LoggerFactory.getLogger(ContextAwarePostProcessor.class);

    private final RecommendationConfig config;

    public ContextAwarePostProcessor(RecommendationConfig config) {
        this.config = config;
    }

    public Map<Long, Double> apply(Map<Long, Double> candidateScores,
                                   RecommendationContext context,
                                   Map<Long, ScenicSpot> spotMap) {
        if (candidateScores.isEmpty()) return candidateScores;

        Map<Long, Double> filteredScores = new HashMap<>();

        for (Map.Entry<Long, Double> entry : candidateScores.entrySet()) {
            Long spotId = entry.getKey();
            ScenicSpot spot = spotMap.get(spotId);
            if (spot == null) continue;

            double contextScore = calculateContextScore(spot, context);
            if (contextScore < 0.3) continue;

            double baseScore = entry.getValue();
            double finalScore = baseScore * (1.0 - config.getContextWeight()) + contextScore * config.getContextWeight();
            filteredScores.put(spotId, finalScore);
        }

        logger.info("Context filtering completed: inputCount={}, outputCount={}",
                candidateScores.size(), filteredScores.size());
        return filteredScores;
    }

    // PLACEHOLDER_CONTEXT_METHODS

    private double calculateContextScore(ScenicSpot spot, RecommendationContext context) {
        double score = 1.0;

        if (context.getSeason() != null && isSeasonalMatch(spot, context.getSeason())) {
            score *= config.getSeasonalMatchBoost();
        }

        if (context.getWeather() != null && isWeatherSuitable(spot, context.getWeather())) {
            score *= config.getWeatherMatchBoost();
        }

        if (context.getConsiderDistance() != null && context.getConsiderDistance()
                && context.getCurrentLatitude() != null && context.getCurrentLongitude() != null
                && spot.getLatitude() != null && spot.getLongitude() != null) {
            double distance = calculateDistance(
                    context.getCurrentLatitude(), context.getCurrentLongitude(),
                    spot.getLatitude().doubleValue(), spot.getLongitude().doubleValue());
            double distanceScore = 1.0 / (1.0 + distance / 100.0);
            score *= (1.0 + distanceScore * config.getDistanceBoostFactor());
        }

        if (context.getConsiderBudget() != null && context.getConsiderBudget()
                && context.getBudget() != null && spot.getTicketPrice() != null) {
            double ticketPrice = spot.getTicketPrice().doubleValue();
            if (ticketPrice <= context.getBudget()) {
                score *= 1.1;
            } else {
                score *= config.getBudgetPenalty();
            }
        }

        if (context.getCompanion() != null && isCompanionSuitable(spot, context.getCompanion())) {
            score *= config.getCompanionMatchBoost();
        }

        if (context.getPreferredActivities() != null && !context.getPreferredActivities().isEmpty()) {
            double activityMatch = calculateActivityMatch(spot, context.getPreferredActivities());
            score *= (1.0 + activityMatch * 0.2);
        }

        return Math.min(score, 2.0);
    }

    private boolean isSeasonalMatch(ScenicSpot spot, String season) {
        String resolved = season;
        if (resolved == null) {
            int month = java.time.LocalDateTime.now().getMonthValue();
            if (month >= 3 && month <= 5) resolved = "SPRING";
            else if (month >= 6 && month <= 8) resolved = "SUMMER";
            else if (month >= 9 && month <= 11) resolved = "AUTUMN";
            else resolved = "WINTER";
        }
        switch (resolved.toUpperCase()) {
            case "SPRING": case "SUMMER": case "AUTUMN": return true;
            case "WINTER":
                return spot.getCategory() == ScenicSpot.Category.CULTURAL
                        || spot.getCategory() == ScenicSpot.Category.RELIGIOUS;
            default: return true;
        }
    }

    private boolean isWeatherSuitable(ScenicSpot spot, String weather) {
        if (weather == null) return true;
        switch (weather.toUpperCase()) {
            case "SUNNY": case "CLOUDY": return true;
            case "RAINY":
                return spot.getCategory() == ScenicSpot.Category.CULTURAL
                        || spot.getCategory() == ScenicSpot.Category.RELIGIOUS;
            case "SNOWY":
                return spot.getCategory() == ScenicSpot.Category.CULTURAL
                        || spot.getCategory() == ScenicSpot.Category.RELIGIOUS
                        || (spot.getAltitude() != null && parseAltitude(spot.getAltitude()) > 4000);
            default: return true;
        }
    }

    private boolean isCompanionSuitable(ScenicSpot spot, String companion) {
        if (companion == null) return true;
        switch (companion.toUpperCase()) {
            case "ALONE": case "FRIENDS": case "GROUP": return true;
            case "COUPLE":
                return spot.getCategory() == ScenicSpot.Category.NATURAL
                        || spot.getCategory() == ScenicSpot.Category.CULTURAL;
            case "FAMILY":
                return spot.getCategory() == ScenicSpot.Category.CULTURAL
                        || spot.getCategory() == ScenicSpot.Category.RELIGIOUS;
            default: return true;
        }
    }

    private double calculateActivityMatch(ScenicSpot spot, String preferredActivities) {
        if (preferredActivities == null || preferredActivities.isEmpty()) return 0.0;

        String[] activities = preferredActivities.split(",");
        java.util.List<String> spotTags = extractTagValues(spot.getTags());

        int matchCount = 0;
        for (String activity : activities) {
            String trimmed = activity.trim().toUpperCase();
            for (String tag : spotTags) {
                if (tag.toUpperCase().contains(trimmed) || matchesActivityTag(trimmed, tag)) {
                    matchCount++;
                    break;
                }
            }
        }
        return activities.length > 0 ? (double) matchCount / activities.length : 0.0;
    }

    private boolean matchesActivityTag(String activity, String tag) {
        Map<String, String[]> activityKeywords = new HashMap<>();
        activityKeywords.put("PHOTOGRAPHY", new String[]{"摄影", "拍照", "风景", "美景"});
        activityKeywords.put("HIKING", new String[]{"徒步", "登山", "户外"});
        activityKeywords.put("CULTURE", new String[]{"文化", "历史", "人文"});
        activityKeywords.put("RELIGION", new String[]{"宗教", "寺庙", "朝圣"});
        activityKeywords.put("NATURE", new String[]{"自然", "风光", "山水"});

        String[] keywords = activityKeywords.get(activity);
        if (keywords == null) return false;
        for (String keyword : keywords) {
            if (tag.contains(keyword)) return true;
        }
        return false;
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    private double parseAltitude(String altitude) {
        if (altitude == null || altitude.isEmpty()) return 0.0;
        try {
            String numeric = altitude.replaceAll("[^0-9.]", "");
            return Double.parseDouble(numeric);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private java.util.List<String> extractTagValues(java.util.List<com.tibet.tourism.modules.spot.domain.SpotTag> tags) {
        if (tags == null) return java.util.Collections.emptyList();
        return tags.stream()
                .map(com.tibet.tourism.modules.spot.domain.SpotTag::getTag)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toList());
    }
}
