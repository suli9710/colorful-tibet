package com.tibet.tourism.modules.recommendation.application.strategy;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.domain.SpotTag;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class DiversityReranker {

    private final RecommendationConfig config;
    private final ScenicSpotRepository spotRepository;

    public DiversityReranker(RecommendationConfig config, ScenicSpotRepository spotRepository) {
        this.config = config;
        this.spotRepository = spotRepository;
    }

    public List<ScenicSpot> rerank(Map<Long, Double> candidateScores,
                                   Set<Long> visitedSpotIds,
                                   Map<Long, ScenicSpot> preloadedSpotMap) {
        if (candidateScores.isEmpty()) return Collections.emptyList();

        List<Long> remainingCandidateIds = new ArrayList<>(candidateScores.keySet());
        Map<Long, ScenicSpot> spotMap = preloadedSpotMap;
        if (spotMap == null) {
            spotMap = spotRepository.findAllById(remainingCandidateIds).stream()
                    .collect(Collectors.toMap(ScenicSpot::getId, spot -> spot));
        }

        List<Long> selectedSpots = new ArrayList<>();
        Map<Long, Double> maxSimToSelected = new HashMap<>();
        Random random = new Random();

        for (int i = 0; i < config.getMaxResults() && !remainingCandidateIds.isEmpty(); i++) {
            if (random.nextDouble() < config.getExplorationRate()) {
                Long randomSpotId = remainingCandidateIds.get(random.nextInt(remainingCandidateIds.size()));
                ScenicSpot randomSpot = spotMap.get(randomSpotId);
                if (randomSpot != null) {
                    selectedSpots.add(randomSpotId);
                    remainingCandidateIds.remove(randomSpotId);
                    updateMaxSimAfterSelect(randomSpot, remainingCandidateIds, spotMap, maxSimToSelected);
                    continue;
                }
            }

            Long bestSpotId = null;
            double bestScore = -Double.MAX_VALUE;
            for (Long spotId : remainingCandidateIds) {
                double baseScore = candidateScores.getOrDefault(spotId, 0.0);
                double penalty = maxSimToSelected.getOrDefault(spotId, 0.0) * config.getDiversityPenalty();
                double adjusted = baseScore * (1.0 - penalty);
                if (adjusted > bestScore) {
                    bestScore = adjusted;
                    bestSpotId = spotId;
                }
            }

            if (bestSpotId == null) break;
            ScenicSpot bestSpot = spotMap.get(bestSpotId);
            selectedSpots.add(bestSpotId);
            remainingCandidateIds.remove(bestSpotId);
            if (bestSpot != null) {
                updateMaxSimAfterSelect(bestSpot, remainingCandidateIds, spotMap, maxSimToSelected);
            }
        }

        Map<Long, ScenicSpot> finalSpotMap = spotMap;
        return selectedSpots.stream()
                .map(finalSpotMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private void updateMaxSimAfterSelect(ScenicSpot justSelected,
                                         List<Long> remaining,
                                         Map<Long, ScenicSpot> spotMap,
                                         Map<Long, Double> maxSimToSelected) {
        for (Long spotId : remaining) {
            ScenicSpot candidate = spotMap.get(spotId);
            if (candidate == null) continue;
            double tagSim = calculateTagSimilarity(candidate, justSelected);
            double catSim = candidate.getCategory() == justSelected.getCategory() ? 1.0 : 0.0;
            double sim = 0.7 * tagSim + 0.3 * catSim;
            maxSimToSelected.merge(spotId, sim, Math::max);
        }
    }

    private double calculateTagSimilarity(ScenicSpot spot1, ScenicSpot spot2) {
        List<String> tags1 = extractTagValues(spot1.getTags());
        List<String> tags2 = extractTagValues(spot2.getTags());
        if (tags1.isEmpty() || tags2.isEmpty()) return 0.0;

        Set<String> set1 = new HashSet<>(tags1);
        Set<String> set2 = new HashSet<>(tags2);

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    private List<String> extractTagValues(List<SpotTag> tags) {
        if (tags == null) return Collections.emptyList();
        return tags.stream()
                .map(SpotTag::getTag)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }
}
