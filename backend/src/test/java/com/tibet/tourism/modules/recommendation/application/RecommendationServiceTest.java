package com.tibet.tourism.modules.recommendation.application;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tibet.tourism.modules.recommendation.application.strategy.ContentBasedStrategy;
import com.tibet.tourism.modules.recommendation.application.strategy.ContextAwarePostProcessor;
import com.tibet.tourism.modules.recommendation.application.strategy.DiversityReranker;
import com.tibet.tourism.modules.recommendation.application.strategy.RecommendationCacheService;
import com.tibet.tourism.modules.recommendation.application.strategy.RecommendationConfig;
import com.tibet.tourism.modules.recommendation.application.strategy.UserBasedCFStrategy;
import com.tibet.tourism.modules.recommendation.web.dto.RecommendationContext;
import com.tibet.tourism.modules.recommendation.web.dto.RecommendationDebugResponse;
import com.tibet.tourism.modules.spot.application.CompanionInferenceService;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.domain.SpotTag;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import com.tibet.tourism.modules.spot.web.dto.ScenicSpotResponse;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.domain.UserVisitHistory;
import com.tibet.tourism.modules.user.infra.UserVisitHistoryRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.Test;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.Mock;
import org.springframework.data.domain.PageImpl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Mock private UserVisitHistoryRepository historyRepository;
    @Mock private ScenicSpotRepository spotRepository;
    @Mock private CompanionInferenceService companionInferenceService;
    @Mock private ItemBasedRecommendationService itemBasedRecommendationService;
    @Mock private ColdStartOptimizationService coldStartOptimizationService;
    @Mock private UserBasedCFStrategy userBasedCFStrategy;
    @Mock private ContentBasedStrategy contentBasedStrategy;
    @Mock private ContextAwarePostProcessor contextAwarePostProcessor;
    @Mock private DiversityReranker diversityReranker;
    @Mock private RecommendationCacheService cacheService;

    private RecommendationService recommendationService;
    private RecommendationConfig config;

    private User testUser;
    private ScenicSpot spot1, spot2, spot3, spot4, spot5, spot6;
    private List<UserVisitHistory> userHistories;

    @BeforeEach
    void setUp() {
        config = new RecommendationConfig();
        recommendationService = new RecommendationService(
                historyRepository,
                spotRepository,
                companionInferenceService,
                itemBasedRecommendationService,
                coldStartOptimizationService,
                userBasedCFStrategy,
                contentBasedStrategy,
                contextAwarePostProcessor,
                diversityReranker,
                cacheService,
                config);

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");

        spot1 = createSpot(1L, "布达拉宫", ScenicSpot.Category.CULTURAL, 15000, 4.8, "29.6578", "91.1172");
        spot2 = createSpot(2L, "大昭寺", ScenicSpot.Category.CULTURAL, 12000, 4.7, "29.6533", "91.1322");
        spot3 = createSpot(3L, "纳木错", ScenicSpot.Category.NATURAL, 11000, 4.6, "30.7667", "90.5667");
        spot4 = createSpot(4L, "羊卓雍措", ScenicSpot.Category.NATURAL, 10000, 4.5, "28.9333", "90.6833");
        spot5 = createSpot(5L, "鲁朗林海", ScenicSpot.Category.NATURAL, 5000, 4.2, "29.7333", "94.7333");
        spot6 = createSpot(6L, "冈仁波齐", ScenicSpot.Category.CULTURAL, 3000, 4.0, "31.0667", "81.3125");

        userHistories = Arrays.asList(
                createHistory(testUser, spot1, 5, 6, 240, LocalDateTime.now().minusDays(10)),
                createHistory(testUser, spot2, 4, 3, 120, LocalDateTime.now().minusDays(30)),
                createHistory(testUser, spot3, 5, 8, 360, LocalDateTime.now().minusDays(60))
        );
    }

    @Test
    void testRecommendForUserWithHistory() {
        when(historyRepository.findByUserId(1L)).thenReturn(userHistories);
        when(companionInferenceService.getCompanionType(1L)).thenReturn("ALONE");
        when(contentBasedStrategy.getTagProfile(eq(1L), anyList(), anySet())).thenReturn(Map.of("宫殿", 5.0));
        when(userBasedCFStrategy.score(eq(1L), anyList(), anySet())).thenReturn(Map.of(4L, 0.8));
        when(itemBasedRecommendationService.recommendByItemCF(eq(1L), anySet())).thenReturn(Collections.emptyMap());
        when(contentBasedStrategy.score(eq(1L), anyList(), anySet())).thenReturn(Map.of(4L, 0.5));
        when(diversityReranker.rerank(anyMap(), anySet(), any())).thenReturn(List.of(spot4));

        List<ScenicSpot> result = recommendationService.recommendSpotsForUser(1L);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals(spot4.getId(), result.get(0).getId());
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
        when(contentBasedStrategy.getTagProfile(eq(1L), anyList(), anySet())).thenReturn(Map.of("宫殿", 5.0));
        when(userBasedCFStrategy.score(eq(1L), anyList(), anySet())).thenReturn(Map.of(4L, 0.8));
        when(itemBasedRecommendationService.recommendByItemCF(eq(1L), anySet())).thenReturn(Collections.emptyMap());
        when(contentBasedStrategy.score(eq(1L), anyList(), anySet())).thenReturn(Map.of(4L, 0.5));
        when(spotRepository.findAllById(anyList())).thenReturn(List.of(spot4));
        when(contextAwarePostProcessor.apply(anyMap(), any(RecommendationContext.class), anyMap()))
                .thenReturn(Map.of(4L, 0.9));
        when(diversityReranker.rerank(anyMap(), anySet(), any())).thenReturn(List.of(spot4));

        RecommendationContext context = new RecommendationContext();
        context.setSeason("WINTER");
        context.setConsiderDistance(false);
        context.setConsiderBudget(false);

        List<ScenicSpot> result = recommendationService.recommendSpotsForUser(1L, context);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(contextAwarePostProcessor).apply(anyMap(), any(RecommendationContext.class), anyMap());
    }

    @Test
    void testRecommendWithDebug() {
        when(historyRepository.findByUserId(1L)).thenReturn(userHistories);
        when(companionInferenceService.getCompanionType(1L)).thenReturn("ALONE");
        when(contentBasedStrategy.getTagProfile(eq(1L), anyList(), anySet())).thenReturn(Map.of("宫殿", 5.0));
        when(userBasedCFStrategy.score(eq(1L), anyList(), anySet())).thenReturn(Map.of(4L, 0.8));
        when(itemBasedRecommendationService.recommendByItemCF(eq(1L), anySet())).thenReturn(Collections.emptyMap());
        when(contentBasedStrategy.score(eq(1L), anyList(), anySet())).thenReturn(Map.of(4L, 0.5));
        when(diversityReranker.rerank(anyMap(), anySet(), any())).thenReturn(List.of(spot4));
        when(spotRepository.findAllById(anyList())).thenReturn(List.of(spot4));
        spot4.setNum(99);
        spot4.setCreatedAt(LocalDateTime.parse("2026-01-02T03:04:05"));

        RecommendationDebugResponse response = recommendationService.recommendWithDebug(1L);

        assertNotNull(response);
        assertEquals(1L, response.getUserId());
        assertFalse(response.isFallbackUsed());
        assertTrue(response.isHasHistory());
        assertNotNull(response.getRecommendations());
        assertInstanceOf(ScenicSpotResponse.class, response.getRecommendations().get(0));
        assertNotNull(response.getAlgorithmConfig());
        assertTrue(response.getComputationTimeMs() >= 0);
    }

    @Test
    void testRecommendWithDebugSerializesRecommendationsAsPublicDtos() throws Exception {
        when(historyRepository.findByUserId(1L)).thenReturn(userHistories);
        when(companionInferenceService.getCompanionType(1L)).thenReturn("ALONE");
        when(contentBasedStrategy.getTagProfile(eq(1L), anyList(), anySet())).thenReturn(Map.of("瀹", 5.0));
        when(userBasedCFStrategy.score(eq(1L), anyList(), anySet())).thenReturn(Map.of(4L, 0.8));
        when(itemBasedRecommendationService.recommendByItemCF(eq(1L), anySet())).thenReturn(Collections.emptyMap());
        when(contentBasedStrategy.score(eq(1L), anyList(), anySet())).thenReturn(Map.of(4L, 0.5));
        when(diversityReranker.rerank(anyMap(), anySet(), any())).thenReturn(List.of(spot4));
        when(spotRepository.findAllById(anyList())).thenReturn(List.of(spot4));
        spot4.setNum(99);
        spot4.setCreatedAt(LocalDateTime.parse("2026-01-02T03:04:05"));

        RecommendationDebugResponse response = recommendationService.recommendWithDebug(1L, null, "zh");

        String json = OBJECT_MAPPER.writeValueAsString(response);
        assertTrue(json.contains("\"recommendations\""));
        assertFalse(json.contains("\"num\""));
        assertFalse(json.contains("\"createdAt\""));
    }

    @Test
    void testCacheInvalidation() {
        recommendationService.invalidateUserCache(1L);
        verify(cacheService).invalidateUserCache(1L);
    }

    @Test
    void testFallbackWhenNoVisitedSpotIds() {
        List<UserVisitHistory> badHistory = List.of(createHistory(testUser, new ScenicSpot(), 3, 1, 60, LocalDateTime.now()));
        when(historyRepository.findByUserId(1L)).thenReturn(badHistory);
        lenient().when(companionInferenceService.getCompanionType(1L)).thenReturn("ALONE");
        when(spotRepository.findAllWithoutTags(any())).thenReturn(new PageImpl<>(List.of(spot1, spot2, spot3)));

        List<ScenicSpot> result = recommendationService.recommendSpotsForUser(1L);

        assertNotNull(result);
        assertFalse(result.isEmpty());
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
