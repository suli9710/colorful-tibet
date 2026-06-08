package com.tibet.tourism.modules.recommendation.application.strategy;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.domain.UserVisitHistory;
import com.tibet.tourism.modules.user.infra.UserVisitHistoryRepository;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserBasedCFStrategyTest {

    @Mock private UserVisitHistoryRepository historyRepository;
    @Mock private RecommendationCacheService cacheService;

    private UserBasedCFStrategy strategy;

    @BeforeEach
    void setUp() {
        RecommendationConfig config = new RecommendationConfig();
        strategy = new UserBasedCFStrategy(historyRepository, cacheService, config);
    }

    @Test
    void scoreUsesRecentTopNRepositoryLookupsForCollaborativeHistory() {
        User currentUser = user(1L);
        User similarUser = user(2L);
        ScenicSpot spot1 = spot(1L);
        ScenicSpot spot2 = spot(2L);
        ScenicSpot spot3 = spot(3L);
        List<UserVisitHistory> currentHistory = List.of(
                history(currentUser, spot1, 5),
                history(currentUser, spot2, 4));
        List<Long> visitedSpotIds = List.of(1L, 2L);

        when(historyRepository.findRecentBySpotIdIn(visitedSpotIds)).thenReturn(List.of(
                history(similarUser, spot1, 5),
                history(similarUser, spot2, 4)));
        when(cacheService.getSimilarity(1L)).thenReturn(null);
        when(historyRepository.findRecentByUserIdIn(List.of(2L))).thenReturn(List.of(
                history(similarUser, spot1, 5),
                history(similarUser, spot2, 4),
                history(similarUser, spot3, 5)));

        Map<Long, Double> scores = strategy.score(
                1L,
                currentHistory,
                new LinkedHashSet<>(visitedSpotIds));

        assertTrue(scores.containsKey(3L));
        verify(historyRepository).findRecentBySpotIdIn(visitedSpotIds);
        verify(historyRepository).findRecentByUserIdIn(List.of(2L));
        verify(historyRepository, never()).findBySpotIdIn(anyList());
        verify(historyRepository, never()).findByUserIdIn(anyList());
    }

    private static User user(Long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private static ScenicSpot spot(Long id) {
        ScenicSpot spot = new ScenicSpot();
        spot.setId(id);
        return spot;
    }

    private static UserVisitHistory history(User user, ScenicSpot spot, int rating) {
        UserVisitHistory history = new UserVisitHistory();
        history.setUser(user);
        history.setSpot(spot);
        history.setRating(rating);
        history.setClickCount(1);
        history.setDwellSeconds(60);
        history.setVisitDate(LocalDateTime.now().minusDays(1));
        return history;
    }
}
