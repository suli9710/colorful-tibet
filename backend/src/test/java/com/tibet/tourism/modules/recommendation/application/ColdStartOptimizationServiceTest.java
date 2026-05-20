package com.tibet.tourism.modules.recommendation.application;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.domain.UserVisitHistory;
import com.tibet.tourism.modules.user.infra.UserRepository;
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
import org.springframework.data.domain.PageImpl;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ColdStartOptimizationServiceTest {

    @Mock private ScenicSpotRepository spotRepository;
    @Mock private UserRepository userRepository;
    @Mock private UserVisitHistoryRepository historyRepository;
    @Mock private ItemBasedRecommendationService itemBasedRecommendationService;

    @InjectMocks
    private ColdStartOptimizationService coldStartService;

    private User testUser;
    private ScenicSpot spot1, spot2, spot3, spot4, spot5;
    private List<ScenicSpot> allSpots;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setCity("拉萨");

        spot1 = createSpot(1L, "布达拉宫", ScenicSpot.Category.CULTURAL, "拉萨", 15000);
        spot2 = createSpot(2L, "大昭寺", ScenicSpot.Category.CULTURAL, "拉萨", 12000);
        spot3 = createSpot(3L, "纳木错", ScenicSpot.Category.NATURAL, "那曲", 11000);
        spot4 = createSpot(4L, "羊卓雍措", ScenicSpot.Category.NATURAL, "山南", 10000);
        spot5 = createSpot(5L, "扎什伦布寺", ScenicSpot.Category.CULTURAL, "日喀则", 8000);

        allSpots = Arrays.asList(spot1, spot2, spot3, spot4, spot5);
    }

    @Test
    void testIsNewUser() {
        when(historyRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        assertTrue(coldStartService.isNewUser(1L));

        List<UserVisitHistory> oneHistory = Arrays.asList(
                createHistory(testUser, spot1, 5, LocalDateTime.now())
        );
        when(historyRepository.findByUserId(1L)).thenReturn(oneHistory);
        assertTrue(coldStartService.isNewUser(1L));

        List<UserVisitHistory> threeHistories = Arrays.asList(
                createHistory(testUser, spot1, 5, LocalDateTime.now()),
                createHistory(testUser, spot2, 4, LocalDateTime.now()),
                createHistory(testUser, spot3, 5, LocalDateTime.now())
        );
        when(historyRepository.findByUserId(1L)).thenReturn(threeHistories);
        assertFalse(coldStartService.isNewUser(1L));
    }

    @Test
    void testRecommendForNewUserByAttributes() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        mockSpotCache(allSpots);

        List<ScenicSpot> result = coldStartService.recommendForNewUserByAttributes(1L);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        // 拉萨用户应优先获得拉萨的景点
        boolean hasLhasaSpot = result.stream()
                .anyMatch(s -> s.getLocation() != null && s.getLocation().contains("拉萨"));
        assertTrue(hasLhasaSpot, "拉萨用户应获得拉萨地区的推荐");
    }

    @Test
    void testRecommendForNewUserByAttributesNoUser() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());
        List<ScenicSpot> result = coldStartService.recommendForNewUserByAttributes(99L);
        assertTrue(result.isEmpty());
    }

    @Test
    void testRecommendForNewUserByPreferences() {
        // 模拟标签匹配
        mockSpotCache(allSpots);
        // 类别匹配
        when(spotRepository.findByCategory(eq(ScenicSpot.Category.CULTURAL), any()))
                .thenReturn(new PageImpl<>(Arrays.asList(spot1, spot2, spot5)));

        List<String> preferredTags = Arrays.asList("佛教", "宫殿");
        List<ScenicSpot> result = coldStartService.recommendForNewUserByPreferences(
                1L, preferredTags, "CULTURAL", "ALONE");

        assertNotNull(result);
        assertFalse(result.isEmpty());
        verify(spotRepository, times(1)).findByCategory(eq(ScenicSpot.Category.CULTURAL), any());
    }

    @Test
    void testRecommendForNewUserByLocation() {
        mockSpotCache(allSpots);

        // 查询拉萨附近的景点（拉萨坐标: 29.65, 91.13）
        List<ScenicSpot> result = coldStartService.recommendForNewUserByLocation(
                29.65, 91.13, 50.0);

        assertNotNull(result);
        assertFalse(result.isEmpty());
        // spot1和spot2在拉萨，应在结果中
    }

    @Test
    void testRecommendForNewUserByLocationNullCoords() {
        List<ScenicSpot> result = coldStartService.recommendForNewUserByLocation(null, null, 50.0);
        assertTrue(result.isEmpty());
    }

    @Test
    void testHybridColdStartRecommendation() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        mockSpotCache(allSpots);
        when(spotRepository.findByCategory(eq(ScenicSpot.Category.CULTURAL), any()))
                .thenReturn(new PageImpl<>(Arrays.asList(spot1, spot2, spot5)));
        when(spotRepository.findAllById(anyList())).thenAnswer(inv -> {
            List<Long> ids = inv.getArgument(0);
            return allSpots.stream().filter(s -> ids.contains(s.getId())).toList();
        });

        List<ScenicSpot> result = coldStartService.hybridColdStartRecommendation(
                1L, 29.65, 91.13, Arrays.asList("佛教"), "CULTURAL", "ALONE");

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    @Test
    void testHybridColdStartFallbackToPopular() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        mockSpotCache(allSpots);
        when(spotRepository.findAllById(anyList())).thenAnswer(inv -> {
            List<Long> ids = inv.getArgument(0);
            return allSpots.stream().filter(s -> ids.contains(s.getId())).toList();
        });

        // 不给位置和偏好，只靠用户属性
        List<ScenicSpot> result = coldStartService.hybridColdStartRecommendation(
                1L, null, null, null, null, null);

        assertNotNull(result);
        assertFalse(result.isEmpty());
    }

    // --- helpers ---

    private void mockSpotCache(List<ScenicSpot> spots) {
        when(spotRepository.findAllWithoutTags(any())).thenReturn(new PageImpl<>(spots));
        when(spotRepository.findByIdInWithTags(anyList())).thenAnswer(inv -> {
            List<Long> ids = inv.getArgument(0);
            return spots.stream().filter(s -> ids.contains(s.getId())).toList();
        });
    }

    private ScenicSpot createSpot(Long id, String name, ScenicSpot.Category category, String location, int visitCount) {
        ScenicSpot spot = new ScenicSpot();
        spot.setId(id);
        spot.setName(name);
        spot.setCategory(category);
        spot.setLocation(location);
        spot.setVisitCount(visitCount);
        spot.setRating(BigDecimal.valueOf(4.0));
        spot.setLatitude(new BigDecimal("29.6"));
        spot.setLongitude(new BigDecimal("91.1"));
        spot.setTicketPrice(BigDecimal.valueOf(60));
        return spot;
    }

    private UserVisitHistory createHistory(User user, ScenicSpot spot, int rating, LocalDateTime visitDate) {
        UserVisitHistory history = new UserVisitHistory();
        history.setUser(user);
        history.setSpot(spot);
        history.setRating(rating);
        history.setClickCount(0);
        history.setDwellSeconds(0);
        history.setVisitDate(visitDate);
        return history;
    }
}
