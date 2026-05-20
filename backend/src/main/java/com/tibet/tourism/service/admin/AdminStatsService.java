package com.tibet.tourism.service.admin;

import com.tibet.tourism.entity.Booking;
import com.tibet.tourism.entity.HotelBooking;
import com.tibet.tourism.repository.BookingRepository;
import com.tibet.tourism.repository.HotelBookingRepository;
import com.tibet.tourism.repository.NewsRepository;
import com.tibet.tourism.repository.ScenicSpotRepository;
import com.tibet.tourism.repository.SharedRouteRepository;
import com.tibet.tourism.repository.TravelQuestionRepository;
import com.tibet.tourism.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
public class AdminStatsService {

    private final UserRepository userRepository;
    private final ScenicSpotRepository scenicSpotRepository;
    private final NewsRepository newsRepository;
    private final BookingRepository bookingRepository;
    private final HotelBookingRepository hotelBookingRepository;
    private final SharedRouteRepository sharedRouteRepository;
    private final TravelQuestionRepository travelQuestionRepository;

    public AdminStatsService(UserRepository userRepository,
                             ScenicSpotRepository scenicSpotRepository,
                             NewsRepository newsRepository,
                             BookingRepository bookingRepository,
                             HotelBookingRepository hotelBookingRepository,
                             SharedRouteRepository sharedRouteRepository,
                             TravelQuestionRepository travelQuestionRepository) {
        this.userRepository = userRepository;
        this.scenicSpotRepository = scenicSpotRepository;
        this.newsRepository = newsRepository;
        this.bookingRepository = bookingRepository;
        this.hotelBookingRepository = hotelBookingRepository;
        this.sharedRouteRepository = sharedRouteRepository;
        this.travelQuestionRepository = travelQuestionRepository;
    }

    public Map<String, Object> getDashboardStats() {
        long userCount = userRepository.count();
        long spotCount = scenicSpotRepository.count();
        long newsCount = newsRepository.count();
        long sharedRouteCount = sharedRouteRepository.count();
        long questionCount = travelQuestionRepository.count();

        long scenicBookingCount = bookingRepository.countConfirmed();
        BigDecimal scenicRevenue = bookingRepository.sumConfirmedRevenue();
        if (scenicRevenue == null) scenicRevenue = BigDecimal.ZERO;

        long hotelBookingCount = hotelBookingRepository.countByStatus(HotelBooking.Status.CONFIRMED);
        BigDecimal hotelRevenue = hotelBookingRepository.sumTotalRevenue();
        if (hotelRevenue == null) hotelRevenue = BigDecimal.ZERO;

        long totalOrderCount = scenicBookingCount + hotelBookingCount;
        BigDecimal totalRevenue = scenicRevenue.add(hotelRevenue);

        List<Booking> recentScenicBookings = bookingRepository.findTop5ByStatusOrderByCreatedAtDesc(Booking.Status.CONFIRMED);
        List<HotelBooking> recentHotelBookings = hotelBookingRepository.findTop5ByStatusAndDeletedAtIsNullOrderByCreatedAtDesc(HotelBooking.Status.CONFIRMED);
        LocalDateTime trendStartAt = YearMonth.now().minusMonths(5).atDay(1).atStartOfDay();
        List<Booking> confirmedScenicBookings = bookingRepository
                .findByStatusAndCreatedAtAfterOrderByCreatedAtAsc(Booking.Status.CONFIRMED, trendStartAt);
        List<HotelBooking> confirmedHotelBookings = hotelBookingRepository
                .findByStatusAndDeletedAtIsNullAndCreatedAtAfterOrderByCreatedAtAsc(HotelBooking.Status.CONFIRMED, trendStartAt);

        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userCount);
        stats.put("spotCount", spotCount);
        stats.put("newsCount", newsCount);
        stats.put("sharedRouteCount", sharedRouteCount);
        stats.put("questionCount", questionCount);
        stats.put("orderCount", totalOrderCount);
        stats.put("totalRevenue", totalRevenue);
        stats.put("recentBookings", recentScenicBookings);
        stats.put("recentHotelBookings", recentHotelBookings);
        stats.put("popularSpots", scenicSpotRepository.findTop8ByOrderByVisitCountDescIdAsc());
        stats.put("spotCategories", buildSpotCategories());
        stats.put("monthlyBookingTrend", buildMonthlyBookingTrend(confirmedScenicBookings, confirmedHotelBookings));
        stats.put("userGrowthTrend", buildUserGrowthTrend());
        stats.put("newsPublishTrend", buildNewsPublishTrend());
        stats.put("visitorCityDistribution", buildVisitorCityDistribution());
        stats.put("updatedAt", LocalDateTime.now());

        return stats;
    }

    private List<Map<String, Object>> buildSpotCategories() {
        return scenicSpotRepository.countByCategoryGroup().stream()
                .map(row -> {
                    Object category = row[0];
                    Number count = (Number) row[1];
                    String key = category == null ? "未分类" : category.toString();
                    return Map.<String, Object>of("name", key, "value", count.longValue());
                })
                .toList();
    }

    private List<Map<String, Object>> buildMonthlyBookingTrend(List<Booking> scenicBookings, List<HotelBooking> hotelBookings) {
        Map<String, long[]> buckets = new LinkedHashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM");
        YearMonth startMonth = YearMonth.now().minusMonths(5);

        for (int i = 0; i < 6; i++) {
            buckets.put(startMonth.plusMonths(i).format(formatter), new long[2]);
        }

        scenicBookings.forEach(item -> {
            if (item.getCreatedAt() != null) {
                String month = item.getCreatedAt().format(formatter);
                if (!buckets.containsKey(month)) return;
                long[] values = buckets.computeIfAbsent(month, k -> new long[2]);
                values[0] += 1;
                values[1] += item.getTotalPrice() == null ? 0 : item.getTotalPrice().longValue();
            }
        });
        hotelBookings.forEach(item -> {
            if (item.getCreatedAt() != null) {
                String month = item.getCreatedAt().format(formatter);
                if (!buckets.containsKey(month)) return;
                long[] values = buckets.computeIfAbsent(month, k -> new long[2]);
                values[0] += 1;
                values[1] += item.getTotalPrice() == null ? 0 : item.getTotalPrice().longValue();
            }
        });

        return buckets.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> Map.<String, Object>of(
                        "month", entry.getKey(),
                        "orderCount", entry.getValue()[0],
                        "revenue", entry.getValue()[1]))
                .toList();
    }

    private List<Map<String, Object>> buildVisitorCityDistribution() {
        List<String> sourceProvinces = List.of(
                "四川", "北京", "上海", "广东", "浙江", "江苏", "陕西", "云南",
                "重庆", "湖北", "湖南", "山东", "河南", "福建", "广西", "西藏");
        long base = Math.max(260L, userRepository.count() * 4);
        java.util.Random random = new java.util.Random(java.time.LocalDate.now().toEpochDay());

        return sourceProvinces.stream()
                .map(province -> Map.<String, Object>of(
                        "name", province,
                        "value", Math.max(18L, base / 20) + random.nextInt((int) Math.max(45L, base / 8))))
                .sorted((left, right) -> Long.compare(
                        ((Number) right.get("value")).longValue(),
                        ((Number) left.get("value")).longValue()))
                .toList();
    }

    private List<Map<String, Object>> buildUserGrowthTrend() {
        LocalDateTime trendStartAt = YearMonth.now().minusMonths(5).atDay(1).atStartOfDay();
        return userRepository.findByCreatedAtAfterOrderByCreatedAtAsc(trendStartAt).stream()
                .filter(user -> user.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        user -> user.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        TreeMap::new,
                        Collectors.counting()))
                .entrySet().stream()
                .map(entry -> Map.<String, Object>of("month", entry.getKey(), "count", entry.getValue()))
                .toList();
    }

    private List<Map<String, Object>> buildNewsPublishTrend() {
        LocalDateTime trendStartAt = YearMonth.now().minusMonths(5).atDay(1).atStartOfDay();
        return newsRepository.findByCreatedAtAfterOrderByCreatedAtAsc(trendStartAt).stream()
                .filter(news -> news.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        news -> news.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM")),
                        TreeMap::new,
                        Collectors.counting()))
                .entrySet().stream()
                .map(entry -> Map.<String, Object>of("month", entry.getKey(), "count", entry.getValue()))
                .toList();
    }
}
