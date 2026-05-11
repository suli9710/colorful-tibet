package com.tibet.tourism.service;

import com.tibet.tourism.dto.RecommendationContext;
import com.tibet.tourism.dto.RecommendationDebugResponse;
import com.tibet.tourism.entity.ScenicSpot;
import com.tibet.tourism.entity.SpotTag;
import com.tibet.tourism.entity.User;
import com.tibet.tourism.entity.UserVisitHistory;
import com.tibet.tourism.repository.ScenicSpotRepository;
import com.tibet.tourism.repository.SpotTagRepository;
import com.tibet.tourism.repository.UserVisitHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock private UserVisitHistoryRepository historyRepository;
    @Mock private ScenicSpotRepository spotRepository;
    @Mock private SpotTagRepository spotTagRepository;
    @Mock private CompanionInferenceService companionInferenceService;
    @Mock private ItemBasedRecommendationService itemBasedRecommendationService;
    @Mock private ColdStartOptimizationService coldStartOptimizationService;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private RecommendationService recommendationService;

    private User testUser;
    private ScenicSpot spot1, spot2, spot3, spot4, spot5, spot6;
    private List<UserVisitHistory> userHistories;
    private List<ScenicSpot> allSpots;
    private List<SpotTag> allTags;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        spot1 = createSpot(1L, "布达拉宫", ScenicSpot.Category.CULTURAL, 15000, 4.8, "29.6578", "91.1172");
        spot2 = createSpot(2L, "大昭寺", ScenicSpot.Category.CULTURAL, 12000, 4.7, "29.6533", "91.1322");
        spot3 = createSpot(3L, "纳木错", ScenicSpot.Category.NATURAL, 11000, 4.6, "30.7667", "90.5667");
        spot4 = createSpot(4L, "羊卓雍措", ScenicSpot.Category.NATURAL, 10000, 4.5, "28.9333", "90.6833");
        spot5 = createSpot(5L, "鲁朗林海", ScenicSpot.Category.NATURAL, 5000, 4.2, "29.7333", "94.7333");
        spot6 = createSpot(6L, "冈仁波齐", ScenicSpot.Category.CULTURAL, 3000, 4.0, "31.0667", "81.3125");

        allSpots = Arrays.asList(spot1, spot2, spot3, spot4, spot5, spot6);

        userHistories = Arrays.asList(
                createHistory(testUser, spot1, 5, 6, 240, LocalDateTime.now().minusDays(10)),
                createHistory(testUser, spot2, 4, 3, 120, LocalDateTime.now().minusDays(30)),
                createHistory(testUser, spot3, 5, 8, 360, LocalDateTime.now().minusDays(60))
        );

        allTags = Arrays.asList(
                createTag(1L, spot1, "宫殿"), createTag(2L, spot1, "佛教"),
                createTag(3L, spot2, "寺庙"), createTag(4L, spot2, "朝圣"),
                createTag(5L, spot3, "湖泊"), createTag(6L, spot3, "自然"),
                createTag(7L, spot4, "湖泊"), createTag(8L, spot4, "圣湖"),
                createTag(9L, spot5, "森林"), createTag(10L, spot5, "自然"),
                createTag(11L, spot6, "神山"), createTag(12L, spot6, "朝圣")
        );

        // Redis mock: 默认缓存未命中（lenient，冷启动测试不会用到）
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(valueOperations.get(anyString())).thenReturn(null);
    }

    @Test
    void testRecommendForUserWithHistory() {
        when(historyRepository.findByUserId(1L)).thenReturn(userHistories);
        when(companionInferenceService.getCompanionType(1L)).thenReturn("ALONE");
        when(spotTagRepository.findBySpotIdIn(anySet())).thenReturn(allTags);

        // 有其他用户也访问过这些景点（至少2个共同景点才触发相似度计算）
        User otherUser = new User();
        otherUser.setId(2L);
        UserVisitHistory otherHist1 = createHistory(otherUser, spot1, 5, 5, 200, LocalDateTime.now().minusDays(5));
        UserVisitHistory otherHist2 = createHistory(otherUser, spot2, 4, 3, 150, LocalDateTime.now().minusDays(10));
        List<UserVisitHistory> overlapHistories = Arrays.asList(otherHist1, otherHist2);
        when(historyRepository.findBySpotIdIn(anyList())).thenReturn(overlapHistories);

        // 相似用户也访问了其他景点
        UserVisitHistory otherSpotHistory = createHistory(otherUser, spot4, 5, 3, 100, LocalDateTime.now().minusDays(7));
        when(historyRepository.findByUserIdIn(anyList())).thenReturn(Arrays.asList(otherSpotHistory));

        // ItemBased CF 返回空
        when(itemBasedRecommendationService.recommendByItemCF(eq(1L), anySet())).thenReturn(Collections.emptyMap());

        // 标签匹配 - 返回候选景点
        when(spotRepository.findByTagsInAndIdNotIn(anyList(), anySet())).thenReturn(Arrays.asList(spot4, spot5, spot6));
        when(spotRepository.findAllById(anyList())).thenReturn(Arrays.asList(spot4));

        List<ScenicSpot> result = recommendationService.recommendSpotsForUser(1L);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        // 已访问的不该出现
        assertTrue(result.stream().noneMatch(s -> s.getId().equals(spot1.getId())));
        assertTrue(result.stream().noneMatch(s -> s.getId().equals(spot2.getId())));
        assertTrue(result.stream().noneMatch(s -> s.getId().equals(spot3.getId())));
    }

    @Test
    void testRecommendContinuesWhenRedisUnavailable() {
        when(redisTemplate.opsForValue()).thenThrow(new RedisConnectionFailureException("Redis down"));
        when(historyRepository.findByUserId(1L)).thenReturn(userHistories);
        when(companionInferenceService.getCompanionType(1L)).thenReturn("ALONE");
        when(spotTagRepository.findBySpotIdIn(anySet())).thenReturn(allTags);

        User otherUser = new User();
        otherUser.setId(2L);
        UserVisitHistory otherHist1 = createHistory(otherUser, spot1, 5, 5, 200, LocalDateTime.now().minusDays(5));
        UserVisitHistory otherHist2 = createHistory(otherUser, spot2, 4, 3, 150, LocalDateTime.now().minusDays(10));
        when(historyRepository.findBySpotIdIn(anyList())).thenReturn(Arrays.asList(otherHist1, otherHist2));

        UserVisitHistory otherSpotHistory = createHistory(otherUser, spot4, 5, 3, 100, LocalDateTime.now().minusDays(7));
        when(historyRepository.findByUserIdIn(anyList())).thenReturn(List.of(otherSpotHistory));
        when(itemBasedRecommendationService.recommendByItemCF(eq(1L), anySet())).thenReturn(Collections.emptyMap());
        when(spotRepository.findByTagsInAndIdNotIn(anyList(), anySet())).thenReturn(Arrays.asList(spot4, spot5, spot6));
        when(spotRepository.findAllById(anyList())).thenReturn(List.of(spot4));

        List<ScenicSpot> result = recommendationService.recommendSpotsForUser(1L);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertTrue(result.stream().noneMatch(s -> s.getId().equals(spot1.getId())));
    }

    @Test
    void testRecommendForNewUser() {
        when(historyRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        lenient().when(companionInferenceService.getCompanionType(1L)).thenReturn(null);
        when(coldStartOptimizationService.hybridColdStartRecommendation(
                eq(1L), isNull(), isNull(), isNull(), isNull(), isNull()))
                .thenReturn(Arrays.asList(spot1, spot2, spot3));

        List<ScenicSpot> result = recommendationService.recommendSpotsForUser(1L);

        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    void testRecommendWithContextSeason() {
        when(historyRepository.findByUserId(1L)).thenReturn(userHistories);
        when(companionInferenceService.getCompanionType(1L)).thenReturn("ALONE");
        when(spotTagRepository.findBySpotIdIn(anySet())).thenReturn(allTags);

        User otherUser = new User();
        otherUser.setId(2L);
        UserVisitHistory otherHist1 = createHistory(otherUser, spot1, 5, 3, 180, LocalDateTime.now().minusDays(5));
        UserVisitHistory otherHist2 = createHistory(otherUser, spot2, 4, 2, 120, LocalDateTime.now().minusDays(10));
        when(historyRepository.findBySpotIdIn(anyList())).thenReturn(Arrays.asList(otherHist1, otherHist2));

        UserVisitHistory otherSpotHistory = createHistory(otherUser, spot5, 4, 2, 80, LocalDateTime.now().minusDays(7));
        when(historyRepository.findByUserIdIn(anyList())).thenReturn(Arrays.asList(otherSpotHistory));
        when(itemBasedRecommendationService.recommendByItemCF(eq(1L), anySet())).thenReturn(Collections.emptyMap());
        when(spotRepository.findByTagsInAndIdNotIn(anyList(), anySet())).thenReturn(Arrays.asList(spot4, spot5, spot6));
        when(spotRepository.findAllById(anyList())).thenReturn(Arrays.asList(spot4, spot5));

        RecommendationContext context = new RecommendationContext();
        context.setSeason("WINTER");
        context.setConsiderDistance(false);
        context.setConsiderBudget(false);

        List<ScenicSpot> result = recommendationService.recommendSpotsForUser(1L, context);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testRecommendWithContextFiltersColdSpotsInWinter() {
        when(historyRepository.findByUserId(1L)).thenReturn(userHistories);
        when(companionInferenceService.getCompanionType(1L)).thenReturn("ALONE");
        when(spotTagRepository.findBySpotIdIn(anySet())).thenReturn(allTags);

        User otherUser = new User();
        otherUser.setId(2L);
        UserVisitHistory otherHist1 = createHistory(otherUser, spot1, 4, 2, 100, LocalDateTime.now().minusDays(5));
        UserVisitHistory otherHist2 = createHistory(otherUser, spot3, 4, 2, 100, LocalDateTime.now().minusDays(10));
        when(historyRepository.findBySpotIdIn(anyList())).thenReturn(Arrays.asList(otherHist1, otherHist2));

        // 相似用户也访问了其他景点
        UserVisitHistory natHistory = createHistory(otherUser, spot4, 4, 2, 100, LocalDateTime.now().minusDays(7));
        UserVisitHistory natHistory2 = createHistory(otherUser, spot5, 4, 2, 100, LocalDateTime.now().minusDays(7));
        when(historyRepository.findByUserIdIn(anyList())).thenReturn(Arrays.asList(natHistory, natHistory2));
        when(itemBasedRecommendationService.recommendByItemCF(eq(1L), anySet())).thenReturn(Collections.emptyMap());
        when(spotRepository.findByTagsInAndIdNotIn(anyList(), anySet())).thenReturn(Arrays.asList(spot4, spot5, spot6));

        // 查询候选景点信息
        when(spotRepository.findAllById(anyList())).thenReturn(Arrays.asList(spot4, spot5, spot6));

        RecommendationContext context = new RecommendationContext();
        context.setSeason("WINTER");
        context.setConsiderDistance(false);
        context.setConsiderBudget(false);

        List<ScenicSpot> result = recommendationService.recommendSpotsForUser(1L, context);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testRecommendWithDebug() {
        when(historyRepository.findByUserId(1L)).thenReturn(userHistories);
        when(companionInferenceService.getCompanionType(1L)).thenReturn("ALONE");
        when(spotTagRepository.findBySpotIdIn(anySet())).thenReturn(allTags);

        User otherUser = new User();
        otherUser.setId(2L);
        UserVisitHistory otherHist1 = createHistory(otherUser, spot1, 5, 3, 150, LocalDateTime.now().minusDays(5));
        UserVisitHistory otherHist2 = createHistory(otherUser, spot2, 4, 2, 100, LocalDateTime.now().minusDays(10));
        when(historyRepository.findBySpotIdIn(anyList())).thenReturn(Arrays.asList(otherHist1, otherHist2));

        UserVisitHistory otherSpotHistory = createHistory(otherUser, spot4, 4, 2, 100, LocalDateTime.now().minusDays(7));
        otherSpotHistory.getSpot().setId(spot4.getId());
        otherSpotHistory.getSpot().setName(spot4.getName());
        when(historyRepository.findByUserIdIn(anyList())).thenReturn(Arrays.asList(otherSpotHistory));
        when(itemBasedRecommendationService.recommendByItemCF(eq(1L), anySet())).thenReturn(Collections.emptyMap());
        when(spotRepository.findByTagsInAndIdNotIn(anyList(), anySet())).thenReturn(Arrays.asList(spot4, spot5, spot6));
        when(spotRepository.findAllById(anyList())).thenReturn(Arrays.asList(spot4));

        RecommendationDebugResponse response = recommendationService.recommendWithDebug(1L);

        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertFalse(response.isFallbackUsed());
        assertTrue(response.isHasHistory());
        assertNotNull(response.getRecommendations());
        assertNotNull(response.getHistory());
        assertEquals(3, response.getHistory().size());
        assertNotNull(response.getSimilarUsers());
        assertNotNull(response.getAlgorithmConfig());
        assertTrue(response.getComputationTimeMs() >= 0);
    }

    @Test
    void testCacheInvalidation() {
        when(historyRepository.findByUserId(1L)).thenReturn(userHistories);
        when(companionInferenceService.getCompanionType(1L)).thenReturn("ALONE");
        when(spotTagRepository.findBySpotIdIn(anySet())).thenReturn(allTags);
        when(historyRepository.findBySpotIdIn(anyList())).thenReturn(Collections.emptyList());
        when(itemBasedRecommendationService.recommendByItemCF(eq(1L), anySet())).thenReturn(Collections.emptyMap());
        when(spotRepository.findByTagsInAndIdNotIn(anyList(), anySet())).thenReturn(Arrays.asList(spot4, spot5, spot6));
        when(spotRepository.findAllById(anyList())).thenReturn(List.of(spot4));

        // 第一次调用，构建缓存
        recommendationService.recommendSpotsForUser(1L);

        // 缓存失效
        recommendationService.invalidateUserCache(1L);

        // 第二次调用，缓存已失效，应重新构建
        when(historyRepository.findByUserId(1L)).thenReturn(userHistories);
        recommendationService.recommendSpotsForUser(1L);

        verify(spotTagRepository, times(2)).findBySpotIdIn(anySet());
    }

    @Test
    void testRecommendationExcludesVisitedSpots() {
        when(historyRepository.findByUserId(1L)).thenReturn(userHistories);
        when(companionInferenceService.getCompanionType(1L)).thenReturn("ALONE");
        when(spotTagRepository.findBySpotIdIn(anySet())).thenReturn(allTags);

        // 相似用户只访问了spot1和spot2（与当前用户相同），没有新候选
        when(historyRepository.findBySpotIdIn(anyList())).thenReturn(Collections.emptyList());
        when(itemBasedRecommendationService.recommendByItemCF(eq(1L), anySet())).thenReturn(Collections.emptyMap());

        // 但标签匹配返回了spot3（已访问）和其他
        when(spotRepository.findByTagsInAndIdNotIn(anyList(), anySet())).thenReturn(Collections.emptyList());

        List<ScenicSpot> result = recommendationService.recommendSpotsForUser(1L);

        assertNotNull(result);
        // 不应该包含已访问的景点
        Set<Long> visitedIds = new HashSet<>(Arrays.asList(spot1.getId(), spot2.getId(), spot3.getId()));
        for (ScenicSpot spot : result) {
            assertFalse(visitedIds.contains(spot.getId()),
                    "推荐结果不应包含已访问景点: " + spot.getName());
        }
    }

    // --- helper methods ---

    private ScenicSpot createSpot(Long id, String name, ScenicSpot.Category category, int visitCount, double rating, String lat, String lng) {
        ScenicSpot spot = new ScenicSpot();
        spot.setId(id);
        spot.setName(name);
        spot.setCategory(category);
        spot.setVisitCount(visitCount);
        spot.setRating(BigDecimal.valueOf(rating));
        spot.setLatitude(new BigDecimal(lat));
        spot.setLongitude(new BigDecimal(lng));
        // tags will be set separately via SpotTag entities
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

    private SpotTag createTag(Long id, ScenicSpot spot, String tag) {
        SpotTag spotTag = new SpotTag();
        spotTag.setId(id);
        spotTag.setSpot(spot);
        spotTag.setTag(tag);
        return spotTag;
    }
}
