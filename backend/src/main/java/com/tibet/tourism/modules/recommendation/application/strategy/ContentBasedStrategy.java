package com.tibet.tourism.modules.recommendation.application.strategy;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.domain.SpotTag;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import com.tibet.tourism.modules.spot.infra.SpotTagRepository;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.domain.UserVisitHistory;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ContentBasedStrategy implements ScoringStrategy {

    private static final Logger logger = LoggerFactory.getLogger(ContentBasedStrategy.class);

    private final SpotTagRepository spotTagRepository;
    private final ScenicSpotRepository spotRepository;
    private final RecommendationCacheService cacheService;
    private final RecommendationConfig config;

    public ContentBasedStrategy(SpotTagRepository spotTagRepository,
                                ScenicSpotRepository spotRepository,
                                RecommendationCacheService cacheService,
                                RecommendationConfig config) {
        this.spotTagRepository = spotTagRepository;
        this.spotRepository = spotRepository;
        this.cacheService = cacheService;
        this.config = config;
    }

    @Override
    public Map<Long, Double> score(Long userId, List<UserVisitHistory> visitHistory, Set<Long> visitedSpotIds) {
        Map<String, Double> tagProfile = cacheService.getTagProfile(userId);
        if (tagProfile == null) {
            tagProfile = buildUserTagProfile(visitHistory, visitedSpotIds);
            cacheService.cacheTagProfile(userId, tagProfile);
            logger.info("Content tag profile built: tagCount={}", tagProfile.size());
        } else {
            logger.info("Content tag profile loaded from cache: tagCount={}", tagProfile.size());
        }

        Map<Long, Double> scores = scoreSpotsByTags(tagProfile, visitedSpotIds);
        logger.info("Content tag candidates generated: candidateCount={}", scores.size());
        return scores;
    }

    public Map<String, Double> getTagProfile(Long userId, List<UserVisitHistory> visitHistory, Set<Long> visitedSpotIds) {
        Map<String, Double> tagProfile = cacheService.getTagProfile(userId);
        if (tagProfile == null) {
            tagProfile = buildUserTagProfile(visitHistory, visitedSpotIds);
            cacheService.cacheTagProfile(userId, tagProfile);
        }
        return tagProfile;
    }

    private Map<String, Double> buildUserTagProfile(List<UserVisitHistory> histories, Set<Long> visitedSpotIds) {
        if (histories.isEmpty() || visitedSpotIds.isEmpty()) return Collections.emptyMap();

        List<SpotTag> spotTags = spotTagRepository.findBySpotIdIn(visitedSpotIds);
        if (spotTags.isEmpty()) return Collections.emptyMap();

        Map<Long, List<String>> tagsBySpot = spotTags.stream()
                .filter(tag -> tag.getSpot() != null && tag.getSpot().getId() != null)
                .collect(Collectors.groupingBy(tag -> tag.getSpot().getId(),
                        Collectors.mapping(SpotTag::getTag, Collectors.toList())));

        Map<String, Double> tagWeights = new HashMap<>();
        for (UserVisitHistory history : histories) {
            Long spotId = history.getSpot().getId();
            List<String> tags = tagsBySpot.get(spotId);
            if (tags == null || tags.isEmpty()) continue;

            double ratingWeight = normalizeRating(history.getRating());
            double recencyWeight = calculateRecencyBoost(history.getVisitDate());
            double engagementWeight = calculateEngagementWeight(history.getClickCount(), history.getDwellSeconds());
            double weight = ratingWeight + recencyWeight + engagementWeight;

            for (String tagValue : tags) {
                if (tagValue == null) continue;
                tagWeights.merge(tagValue, weight, Double::sum);
            }
        }
        return tagWeights;
    }

    private Map<Long, Double> scoreSpotsByTags(Map<String, Double> tagProfile, Set<Long> visitedSpotIds) {
        if (tagProfile.isEmpty()) return Collections.emptyMap();

        List<String> topTags = tagProfile.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        List<ScenicSpot> candidateSpots = spotRepository.findByTagsInAndIdNotIn(topTags, visitedSpotIds);

        return candidateSpots.parallelStream()
                .filter(spot -> spot.getId() != null && !visitedSpotIds.contains(spot.getId()))
                .filter(spot -> spot.getTags() != null && !spot.getTags().isEmpty())
                .collect(Collectors.toConcurrentMap(
                        ScenicSpot::getId,
                        spot -> {
                            List<String> spotTags = extractTagValues(spot.getTags());
                            return spotTags.stream()
                                    .mapToDouble(tag -> tagProfile.getOrDefault(tag, 0.0))
                                    .sum();
                        },
                        (v1, v2) -> v1 + v2))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 0.0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private List<String> extractTagValues(List<SpotTag> tags) {
        if (tags == null) return Collections.emptyList();
        return tags.stream().map(SpotTag::getTag).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private double normalizeRating(Integer rating) {
        return rating == null ? config.getDefaultRating() : rating.doubleValue();
    }

    private double calculateRecencyBoost(LocalDateTime visitDate) {
        if (visitDate == null) return 0.0;
        long days = Math.max(0, Duration.between(visitDate, LocalDateTime.now()).toDays());
        double decay = Math.pow(config.getExponentialDecayFactor(), days / 30.0);
        int month = visitDate.getMonthValue();
        double seasonal = (month >= 5 && month <= 10) ? config.getSeasonalBoost() : 1.0;
        return decay * seasonal;
    }

    private double calculateEngagementWeight(Integer clickCount, Integer dwellSeconds) {
        double clickW = clickCount == null ? 0.0 : Math.log1p(clickCount) * config.getClickWeight();
        double dwellW = dwellSeconds == null ? 0.0 : Math.min(dwellSeconds / 60.0, 5.0) * config.getDwellWeight();
        return clickW + dwellW;
    }
}
