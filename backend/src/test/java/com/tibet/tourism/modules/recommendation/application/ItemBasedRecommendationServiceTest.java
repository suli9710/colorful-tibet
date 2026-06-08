package com.tibet.tourism.modules.recommendation.application;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.domain.UserVisitHistory;
import com.tibet.tourism.modules.user.infra.UserVisitHistoryRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ItemBasedRecommendationServiceTest {

    @Mock private ScenicSpotRepository spotRepository;
    @Mock private UserVisitHistoryRepository historyRepository;

    @InjectMocks
    private ItemBasedRecommendationService itemBasedService;

    private ScenicSpot spot1, spot2, spot3, spot4;
    private User user1, user2, user3;
    private List<ScenicSpot> allSpots;

    @BeforeEach
    void setUp() {
        spot1 = createSpot(1L, "布达拉宫", ScenicSpot.Category.CULTURAL, 15000);
        spot2 = createSpot(2L, "大昭寺", ScenicSpot.Category.CULTURAL, 12000);
        spot3 = createSpot(3L, "纳木错", ScenicSpot.Category.NATURAL, 11000);
        spot4 = createSpot(4L, "羊卓雍措", ScenicSpot.Category.NATURAL, 10000);
        allSpots = Arrays.asList(spot1, spot2, spot3, spot4);

        user1 = new User(); user1.setId(1L);
        user2 = new User(); user2.setId(2L);
        user3 = new User(); user3.setId(3L);
    }

    @Test
    void testPrecomputeItemSimilarityMatrix() {
        when(spotRepository.findAllWithoutTags(any(Pageable.class))).thenReturn(new PageImpl<>(allSpots));

        // user1: spot1(5), spot2(4)
        // user2: spot1(5), spot3(5)
        // user3: spot2(4), spot4(4)
        List<UserVisitHistory> allHistories = Arrays.asList(
                createHistory(user1, spot1, 5, 3, 200, LocalDateTime.now().minusDays(10)),
                createHistory(user1, spot2, 4, 2, 100, LocalDateTime.now().minusDays(20)),
                createHistory(user2, spot1, 5, 4, 300, LocalDateTime.now().minusDays(5)),
                createHistory(user2, spot3, 5, 5, 250, LocalDateTime.now().minusDays(15)),
                createHistory(user3, spot2, 4, 1, 60, LocalDateTime.now().minusDays(30)),
                createHistory(user3, spot4, 4, 2, 120, LocalDateTime.now().minusDays(25))
        );
        when(historyRepository.findRecentForRecommendation(anyInt())).thenReturn(allHistories);

        itemBasedService.precomputeItemSimilarityMatrix();

        // 验证相似度矩阵已构建
        assertFalse(itemBasedService.isSimilarityMatrixStale(),
                "预计算后相似度矩阵不应为空");
    }

    @Test
    void testRecommendByItemCFExcludesVisitedSpots() {
        // 手动预计算
        when(spotRepository.findAllWithoutTags(any(Pageable.class))).thenReturn(new PageImpl<>(allSpots));
        List<UserVisitHistory> allHistories = Arrays.asList(
                createHistory(user1, spot1, 5, 3, 200, LocalDateTime.now().minusDays(10)),
                createHistory(user1, spot2, 4, 2, 100, LocalDateTime.now().minusDays(20)),
                createHistory(user2, spot1, 5, 4, 300, LocalDateTime.now().minusDays(5)),
                createHistory(user2, spot3, 5, 5, 250, LocalDateTime.now().minusDays(15)),
                createHistory(user3, spot2, 4, 1, 60, LocalDateTime.now().minusDays(30)),
                createHistory(user3, spot4, 4, 2, 120, LocalDateTime.now().minusDays(25))
        );
        when(historyRepository.findRecentForRecommendation(anyInt())).thenReturn(allHistories);
        itemBasedService.precomputeItemSimilarityMatrix();

        // 请求用户1的推荐，用户1已经访问了spot1和spot2
        when(historyRepository.findRecentByUserId(1L)).thenReturn(Arrays.asList(
                createHistory(user1, spot1, 5, 3, 200, LocalDateTime.now().minusDays(10)),
                createHistory(user1, spot2, 4, 2, 100, LocalDateTime.now().minusDays(20))
        ));

        Set<Long> visitedSpotIds = new HashSet<>(Arrays.asList(1L, 2L));
        Map<Long, Double> result = itemBasedService.recommendByItemCF(1L, visitedSpotIds);

        assertNotNull(result);
        // spot1和spot2不应在结果中
        assertFalse(result.containsKey(1L), "已访问的spot1不应出现");
        assertFalse(result.containsKey(2L), "已访问的spot2不应出现");
    }

    @Test
    void testRecommendByItemCFEmptyVisitedSet() {
        Map<Long, Double> result = itemBasedService.recommendByItemCF(1L, null);
        assertTrue(result.isEmpty());

        result = itemBasedService.recommendByItemCF(1L, Collections.emptySet());
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetSimilarSpotsEmptyWhenNoMatrix() {
        Map<Long, Double> result = itemBasedService.getSimilarSpots(1L, 5);
        assertTrue(result.isEmpty(), "未预计算时相似景点应为空");
    }

    @Test
    void testIsSimilarityMatrixStaleInitiallyTrue() {
        assertTrue(itemBasedService.isSimilarityMatrixStale(),
                "初始状态下相似度矩阵应为空");
    }

    @Test
    void testPrecomputeWithEmptySpots() {
        when(spotRepository.findAllWithoutTags(any(Pageable.class))).thenReturn(new PageImpl<>(Collections.emptyList()));
        itemBasedService.precomputeItemSimilarityMatrix();
        assertTrue(itemBasedService.isSimilarityMatrixStale());
    }

    // --- helpers ---

    private ScenicSpot createSpot(Long id, String name, ScenicSpot.Category category, int visitCount) {
        ScenicSpot spot = new ScenicSpot();
        spot.setId(id);
        spot.setName(name);
        spot.setCategory(category);
        spot.setVisitCount(visitCount);
        spot.setRating(BigDecimal.valueOf(4.0));
        spot.setLatitude(new BigDecimal("29.6"));
        spot.setLongitude(new BigDecimal("91.1"));
        return spot;
    }

    private UserVisitHistory createHistory(User user, ScenicSpot spot, int rating, int clickCount, int dwellSeconds, LocalDateTime visitDate) {
        UserVisitHistory history = new UserVisitHistory();
        history.setUser(user);
        history.setSpot(spot);
        history.setRating(rating);
        history.setClickCount(clickCount);
        history.setDwellSeconds(dwellSeconds);
        history.setVisitDate(visitDate);
        return history;
    }
}
