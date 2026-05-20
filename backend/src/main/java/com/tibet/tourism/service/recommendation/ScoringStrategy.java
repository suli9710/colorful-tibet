package com.tibet.tourism.service.recommendation;

import com.tibet.tourism.entity.UserVisitHistory;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ScoringStrategy {

    Map<Long, Double> score(Long userId, List<UserVisitHistory> visitHistory, Set<Long> visitedSpotIds);
}
