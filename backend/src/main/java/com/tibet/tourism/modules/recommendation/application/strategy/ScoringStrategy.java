package com.tibet.tourism.modules.recommendation.application.strategy;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.domain.UserVisitHistory;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface ScoringStrategy {

    Map<Long, Double> score(Long userId, List<UserVisitHistory> visitHistory, Set<Long> visitedSpotIds);
}
