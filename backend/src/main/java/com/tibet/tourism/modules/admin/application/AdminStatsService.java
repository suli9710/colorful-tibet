package com.tibet.tourism.modules.admin.application;
import com.tibet.tourism.common.security.PiiMasker;
import com.tibet.tourism.modules.community.infra.SharedRouteRepository;
import com.tibet.tourism.modules.community.infra.TravelQuestionRepository;
import com.tibet.tourism.modules.content.domain.News;
import com.tibet.tourism.modules.content.infra.NewsRepository;
import com.tibet.tourism.modules.hotel.domain.Hotel;
import com.tibet.tourism.modules.hotel.domain.HotelBooking;
import com.tibet.tourism.modules.hotel.infra.HotelBookingRepository;
import com.tibet.tourism.modules.order.domain.Booking;
import com.tibet.tourism.modules.order.infra.BookingRepository;
import com.tibet.tourism.modules.spot.domain.ScenicSpot;
import com.tibet.tourism.modules.spot.infra.ScenicSpotRepository;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.TreeMap;
import org.springframework.stereotype.Service;

@Service
public class AdminStatsService {

    private static final ZoneId BEIJING_ZONE = ZoneId.of("Asia/Shanghai");

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
        LocalDateTime trendStartAt = YearMonth.now(BEIJING_ZONE).minusMonths(5).atDay(1).atStartOfDay();
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
        stats.put("recentBookings", recentScenicBookings.stream().map(this::toRecentScenicBooking).toList());
        stats.put("recentHotelBookings", recentHotelBookings.stream().map(this::toRecentHotelBooking).toList());
        stats.put("popularSpots", scenicSpotRepository.findTop8ByOrderByVisitCountDescIdAsc().stream()
                .map(this::toPopularSpot)
                .toList());
        stats.put("spotCategories", buildSpotCategories());
        stats.put("monthlyBookingTrend", buildMonthlyBookingTrend(confirmedScenicBookings, confirmedHotelBookings));
        stats.put("userGrowthTrend", buildUserGrowthTrend());
        stats.put("newsPublishTrend", buildNewsPublishTrend());
        stats.put("visitorCityDistribution", buildVisitorCityDistribution());
        stats.put("updatedAt", LocalDateTime.now(BEIJING_ZONE));

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
        YearMonth startMonth = YearMonth.now(BEIJING_ZONE).minusMonths(5);

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
        return userRepository.countByCityGroup().stream()
                .map(row -> {
                    String city = row[0] == null || row[0].toString().isBlank() ? "未知" : row[0].toString();
                    Number count = (Number) row[1];
                    return Map.<String, Object>of("name", city, "value", count.longValue());
                })
                .sorted((left, right) -> Long.compare(
                        ((Number) right.get("value")).longValue(),
                        ((Number) left.get("value")).longValue()))
                .toList();
    }

    private List<Map<String, Object>> buildUserGrowthTrend() {
        LocalDateTime trendStartAt = YearMonth.now(BEIJING_ZONE).minusMonths(5).atDay(1).atStartOfDay();
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
        LocalDateTime trendStartAt = YearMonth.now(BEIJING_ZONE).minusMonths(5).atDay(1).atStartOfDay();
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

    private Map<String, Object> toRecentScenicBooking(Booking booking) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", booking.getId());
        data.put("visitDate", booking.getVisitDate());
        data.put("ticketCount", booking.getTicketCount());
        data.put("totalPrice", booking.getTotalPrice());
        data.put("status", booking.getStatus() == null ? null : booking.getStatus().name());
        data.put("createdAt", booking.getCreatedAt());
        data.put("user", toUserSummary(booking.getUser()));
        if (booking.getSpot() == null) {
            data.put("spot", null);
        } else {
            Map<String, Object> spot = new LinkedHashMap<>();
            spot.put("id", booking.getSpot().getId());
            spot.put("name", booking.getSpot().getName());
            data.put("spot", spot);
        }
        return data;
    }

    private Map<String, Object> toRecentHotelBooking(HotelBooking booking) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", booking.getId());
        data.put("roomName", booking.getRoomName());
        data.put("checkInDate", booking.getCheckInDate());
        data.put("checkOutDate", booking.getCheckOutDate());
        data.put("guests", booking.getGuests());
        data.put("guestName", PiiMasker.maskName(booking.getGuestName()));
        data.put("totalPrice", booking.getTotalPrice());
        data.put("status", booking.getStatus() == null ? null : booking.getStatus().name());
        data.put("createdAt", booking.getCreatedAt());
        data.put("user", toUserSummary(booking.getUser()));
        if (booking.getHotel() == null) {
            data.put("hotel", null);
        } else {
            Map<String, Object> hotel = new LinkedHashMap<>();
            hotel.put("id", booking.getHotel().getId());
            hotel.put("name", booking.getHotel().getName());
            data.put("hotel", hotel);
        }
        return data;
    }

    private Map<String, Object> toPopularSpot(com.tibet.tourism.modules.spot.domain.ScenicSpot spot) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", spot.getId());
        data.put("name", spot.getName());
        data.put("location", spot.getLocation());
        data.put("imageUrl", spot.getImageUrl());
        data.put("category", spot.getCategory() == null ? null : spot.getCategory().name());
        data.put("ticketPrice", spot.getTicketPrice());
        data.put("rating", spot.getRating());
        data.put("visitCount", spot.getVisitCount());
        return data;
    }

    private Map<String, Object> toUserSummary(com.tibet.tourism.modules.user.domain.User user) {
        if (user == null) {
            return null;
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        return data;
    }
}
