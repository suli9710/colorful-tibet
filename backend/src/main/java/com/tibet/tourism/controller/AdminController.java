package com.tibet.tourism.controller;

import com.tibet.tourism.entity.AuditLog;
import com.tibet.tourism.entity.Booking;
import com.tibet.tourism.entity.Carousel;
import com.tibet.tourism.entity.Hotel;
import com.tibet.tourism.entity.HotelBooking;
import com.tibet.tourism.entity.News;
import com.tibet.tourism.entity.RoomType;
import com.tibet.tourism.entity.ScenicSpot;
import com.tibet.tourism.entity.TravelRoute;
import com.tibet.tourism.entity.User;
import com.tibet.tourism.repository.AuditLogRepository;
import com.tibet.tourism.repository.BookingRepository;
import com.tibet.tourism.repository.CarouselRepository;
import com.tibet.tourism.repository.CommentLikeRepository;
import com.tibet.tourism.repository.CommentRepository;
import com.tibet.tourism.repository.HotelBookingRepository;
import com.tibet.tourism.repository.HotelRepository;
import com.tibet.tourism.repository.NewsRepository;
import com.tibet.tourism.repository.RoomTypeRepository;
import com.tibet.tourism.repository.RouteCommentRepository;
import com.tibet.tourism.repository.RouteLikeRepository;
import com.tibet.tourism.repository.SharedRouteRepository;
import com.tibet.tourism.repository.TravelRouteRepository;
import com.tibet.tourism.repository.UserVisitHistoryRepository;
import org.springframework.data.domain.Sort;
import com.tibet.tourism.repository.ScenicSpotRepository;
import com.tibet.tourism.repository.UserRepository;
import com.tibet.tourism.service.FileStorageService;
import com.tibet.tourism.service.TibetanTranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 管理员控制器
 * 提供管理员相关的管理功能
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @Autowired
    private ScenicSpotRepository scenicSpotRepository;

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private HotelBookingRepository hotelBookingRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private RouteCommentRepository routeCommentRepository;

    @Autowired
    private CommentLikeRepository commentLikeRepository;

    @Autowired
    private RouteLikeRepository routeLikeRepository;

    @Autowired
    private SharedRouteRepository sharedRouteRepository;

    @Autowired
    private UserVisitHistoryRepository userVisitHistoryRepository;

    @Autowired
    private CarouselRepository carouselRepository;

    @Autowired
    private TravelRouteRepository travelRouteRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private TibetanTranslationService translationService;

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${app.super-admin-username:lzh}")
    private String superAdminUsername;

    /**
     * 获取统计信息（景点门票订单 + 酒店预订 合并统计）
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getStats() {
        long userCount = userRepository.count();
        long spotCount = scenicSpotRepository.count();
        long newsCount = newsRepository.count();

        // 景点门票订单统计（仅已确认的）
        long scenicBookingCount = bookingRepository.countConfirmed();
        BigDecimal scenicRevenue = bookingRepository.sumConfirmedRevenue();
        if (scenicRevenue == null) scenicRevenue = BigDecimal.ZERO;

        // 酒店预订统计（仅已确认的）
        long hotelBookingCount = hotelBookingRepository.countAll(); // 已有 countAll()
        BigDecimal hotelRevenue = hotelBookingRepository.sumTotalRevenue();
        if (hotelRevenue == null) hotelRevenue = BigDecimal.ZERO;

        // 合并统计
        long totalOrderCount = scenicBookingCount + hotelBookingCount;
        BigDecimal totalRevenue = scenicRevenue.add(hotelRevenue);

        // 最新混合订单（景点 + 酒店，各取最新的5条）
        List<Booking> recentScenicBookings = bookingRepository.findTop5ByStatusOrderByCreatedAtDesc(Booking.Status.CONFIRMED);
        List<HotelBooking> recentHotelBookings = hotelBookingRepository.findTop5ByStatusOrderByCreatedAtDesc(HotelBooking.Status.CONFIRMED);

        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userCount);
        stats.put("spotCount", spotCount);
        stats.put("newsCount", newsCount);
        stats.put("orderCount", totalOrderCount);
        stats.put("totalRevenue", totalRevenue);
        stats.put("recentBookings", recentScenicBookings);
        stats.put("recentHotelBookings", recentHotelBookings);
        stats.put("popularSpots", scenicSpotRepository.findAll());
        stats.put("spotCategories", buildSpotCategories());
        stats.put("monthlyBookingTrend", buildMonthlyBookingTrend(recentScenicBookings, recentHotelBookings));
        stats.put("userGrowthTrend", buildUserGrowthTrend());
        stats.put("newsPublishTrend", buildNewsPublishTrend());
        stats.put("updatedAt", java.time.LocalDateTime.now());

        return ResponseEntity.ok(stats);
    }

    /**
     * 获取所有用户
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    @GetMapping("/audit-logs/list")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AuditLog>> getAuditLogs() {
        return ResponseEntity.ok(auditLogRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")));
    }

    @PostMapping("/upload-image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = fileStorageService.storeAdminImage(file);
            return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "上传失败，请稍后重试"));
        }
    }

    /**
     * 更新用户角色
     */
    @PostMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> request) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        User user = userOpt.get();
        String role = request.get("role");
        if (role != null) {
            try {
                user.setRole(User.Role.valueOf(role.toUpperCase()));
                userRepository.save(user);
                return ResponseEntity.ok(Map.of("message", "角色更新成功"));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "无效的角色"));
            }
        }
        return ResponseEntity.badRequest().body(Map.of("error", "角色不能为空"));
    }

    /**
     * 删除用户账户（仅超级管理员可用）
     */
    @DeleteMapping("/users/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id, Authentication authentication) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User targetUser = userOpt.get();
        String operatorUsername = authentication == null ? "anonymous" : authentication.getName();

        if (authentication == null || !superAdminUsername.equals(operatorUsername)) {
            recordAudit(AuditLog.Action.DELETE_USER_DENIED, operatorUsername, targetUser, "非超级管理员尝试删除用户");
            return ResponseEntity.status(403).body(Map.of(
                    "error", "只有超级管理员可以删除用户账户"
            ));
        }

        if (superAdminUsername.equals(targetUser.getUsername())) {
            recordAudit(AuditLog.Action.DELETE_USER_DENIED, operatorUsername, targetUser, "不允许删除超级管理员自身");
            return ResponseEntity.badRequest().body(Map.of("error", "不能删除超级管理员账户"));
        }

        Long userId = targetUser.getId();

        commentLikeRepository.deleteByUserId(userId);

        routeLikeRepository.deleteByUser(targetUser);

        routeCommentRepository.deleteByUser(targetUser);

        commentRepository.deleteByUser(targetUser);

        sharedRouteRepository.deleteByAuthor(targetUser);

        bookingRepository.deleteByUserId(userId);

        hotelBookingRepository.deleteByUserId(userId);

        userVisitHistoryRepository.deleteByUserId(userId);

        recordAudit(AuditLog.Action.DELETE_USER, operatorUsername, targetUser, "成功删除用户: " + targetUser.getUsername());
        userRepository.delete(targetUser);
        return ResponseEntity.ok(Map.of("message", "用户删除成功"));
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getAuditLogs(Authentication authentication) {
        String operatorUsername = authentication == null ? "anonymous" : authentication.getName();
        if (authentication == null || !superAdminUsername.equals(operatorUsername)) {
            return ResponseEntity.status(403).body(Map.of("error", "只有超级管理员可以查看审计日志"));
        }
        return ResponseEntity.ok(auditLogRepository.findTop200ByOrderByCreatedAtDesc());
    }

    private List<Map<String, Object>> buildSpotCategories() {
        Map<String, Long> counts = new HashMap<>();
        scenicSpotRepository.findAll().forEach(spot -> {
            String key = spot.getCategory() == null ? "未分类" : spot.getCategory().name();
            counts.put(key, counts.getOrDefault(key, 0L) + 1);
        });
        return counts.entrySet().stream()
                .map(entry -> Map.<String, Object>of("name", entry.getKey(), "value", entry.getValue()))
                .toList();
    }

    private List<Map<String, Object>> buildMonthlyBookingTrend(List<Booking> scenicBookings, List<HotelBooking> hotelBookings) {
        Map<String, long[]> buckets = new HashMap<>();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM");

        scenicBookings.forEach(item -> {
            if (item.getCreatedAt() != null) {
                String month = item.getCreatedAt().format(formatter);
                long[] values = buckets.computeIfAbsent(month, k -> new long[2]);
                values[0] += 1;
                values[1] += item.getTotalPrice() == null ? 0 : item.getTotalPrice().longValue();
            }
        });
        hotelBookings.forEach(item -> {
            if (item.getCreatedAt() != null) {
                String month = item.getCreatedAt().format(formatter);
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

    private List<Map<String, Object>> buildUserGrowthTrend() {
        return userRepository.findAll(Sort.by(Sort.Direction.ASC, "createdAt")).stream()
                .filter(user -> user.getCreatedAt() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        user -> user.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")),
                        java.util.TreeMap::new,
                        java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .map(entry -> Map.<String, Object>of("month", entry.getKey(), "count", entry.getValue()))
                .toList();
    }

    private List<Map<String, Object>> buildNewsPublishTrend() {
        return newsRepository.findAll(Sort.by(Sort.Direction.ASC, "createdAt")).stream()
                .filter(news -> news.getCreatedAt() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        news -> news.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")),
                        java.util.TreeMap::new,
                        java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .map(entry -> Map.<String, Object>of("month", entry.getKey(), "count", entry.getValue()))
                .toList();
    }

    private void recordAudit(AuditLog.Action action, String operatorUsername, User targetUser, String detail) {
        try {
            AuditLog log = new AuditLog();
            log.setAction(action);
            log.setOperatorUsername(operatorUsername);
            log.setTargetUserId(targetUser.getId());
            log.setTargetUsername(targetUser.getUsername());
            log.setDetail(detail);
            auditLogRepository.save(log);
        } catch (Exception e) {
            System.err.println("审计日志写入失败: " + e.getMessage());
        }
    }

    /**
     * 获取所有景点
     */
    @GetMapping("/spots")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ScenicSpot>> getAllSpots() {
        return ResponseEntity.ok(scenicSpotRepository.findAll());
    }

    /**
     * 更新景点信息
     * 支持自动生成藏语翻译
     */
    @PutMapping("/spots/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateSpot(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Optional<ScenicSpot> spotOpt = scenicSpotRepository.findById(id);
        if (spotOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        ScenicSpot spot = spotOpt.get();
        boolean autoTranslate = request.getOrDefault("autoTranslate", true).equals(true);

        if (request.containsKey("name")) {
            String name = (String) request.get("name");
            spot.setName(name);
            if (autoTranslate && (spot.getNameTibetan() == null || spot.getNameTibetan().isEmpty())) {
                String tibetanName = translationService.translateOrCreate(name, null, com.tibet.tourism.entity.TibetanDictionary.Type.WORD);
                if (tibetanName != null) {
                    spot.setNameTibetan(tibetanName);
                }
            }
        }

        if (request.containsKey("description")) {
            String description = (String) request.get("description");
            spot.setDescription(description);
            if (autoTranslate && (spot.getDescriptionTibetan() == null || spot.getDescriptionTibetan().isEmpty())) {
                String tibetanDesc = translationService.translateDescription(description);
                if (tibetanDesc != null) {
                    spot.setDescriptionTibetan(tibetanDesc);
                }
            }
        }

        if (request.containsKey("nameTibetan")) {
            spot.setNameTibetan((String) request.get("nameTibetan"));
        }
        if (request.containsKey("descriptionTibetan")) {
            spot.setDescriptionTibetan((String) request.get("descriptionTibetan"));
        }
        if (request.containsKey("imageUrl")) {
            spot.setImageUrl((String) request.get("imageUrl"));
        }
        if (request.containsKey("ticketPrice")) {
            Object priceObj = request.get("ticketPrice");
            if (priceObj instanceof Number) {
                spot.setTicketPrice(BigDecimal.valueOf(((Number) priceObj).doubleValue()));
            } else if (priceObj instanceof String) {
                try {
                    spot.setTicketPrice(new BigDecimal((String) priceObj));
                } catch (NumberFormatException e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "无效的价格格式"));
                }
            }
        }
        if (request.containsKey("altitude")) {
            spot.setAltitude((String) request.get("altitude"));
        }
        if (request.containsKey("location")) {
            spot.setLocation((String) request.get("location"));
        }
        if (request.containsKey("category")) {
            try {
                spot.setCategory(ScenicSpot.Category.valueOf(((String) request.get("category")).toUpperCase()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "无效的类别"));
            }
        }
        if (request.containsKey("num")) spot.setNum(((Number) request.get("num")).intValue());
        if (request.containsKey("openInfo")) spot.setOpenInfo((String) request.get("openInfo"));
        if (request.containsKey("entryTime")) spot.setEntryTime((String) request.get("entryTime"));
        if (request.containsKey("latitude")) spot.setLatitude(new BigDecimal(request.get("latitude").toString()));
        if (request.containsKey("longitude")) spot.setLongitude(new BigDecimal(request.get("longitude").toString()));
        if (request.containsKey("altitude")) spot.setAltitude((String) request.get("altitude"));
        if (request.containsKey("location")) spot.setLocation((String) request.get("location"));

        scenicSpotRepository.save(spot);
        return ResponseEntity.ok(spot);
    }

    /**
     * 创建新景点
     */
    @PostMapping("/spots")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createSpot(@RequestBody Map<String, Object> request) {
        ScenicSpot spot = new ScenicSpot();
        boolean autoTranslate = request.getOrDefault("autoTranslate", true).equals(true);

        String name = (String) request.get("name");
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "景点名称不能为空"));
        }
        spot.setName(name);

        if (autoTranslate) {
            String tibetanName = translationService.translateOrCreate(name, null, com.tibet.tourism.entity.TibetanDictionary.Type.WORD);
            if (tibetanName != null) {
                spot.setNameTibetan(tibetanName);
            }
        } else if (request.containsKey("nameTibetan")) {
            spot.setNameTibetan((String) request.get("nameTibetan"));
        }

        if (request.containsKey("description")) {
            String description = (String) request.get("description");
            spot.setDescription(description);
            if (autoTranslate) {
                String tibetanDesc = translationService.translateDescription(description);
                if (tibetanDesc != null) {
                    spot.setDescriptionTibetan(tibetanDesc);
                }
            } else if (request.containsKey("descriptionTibetan")) {
                spot.setDescriptionTibetan((String) request.get("descriptionTibetan"));
            }
        }
        if (request.containsKey("imageUrl")) {
            spot.setImageUrl((String) request.get("imageUrl"));
        }
        if (request.containsKey("ticketPrice")) {
            Object priceObj = request.get("ticketPrice");
            if (priceObj instanceof Number) {
                spot.setTicketPrice(BigDecimal.valueOf(((Number) priceObj).doubleValue()));
            }
        }
        if (request.containsKey("category")) {
            try {
                spot.setCategory(ScenicSpot.Category.valueOf(((String) request.get("category")).toUpperCase()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "无效的类别"));
            }
        }
        if (request.containsKey("num")) spot.setNum(((Number) request.get("num")).intValue());
        if (request.containsKey("openInfo")) spot.setOpenInfo((String) request.get("openInfo"));
        if (request.containsKey("entryTime")) spot.setEntryTime((String) request.get("entryTime"));
        if (request.containsKey("latitude")) spot.setLatitude(new BigDecimal(request.get("latitude").toString()));
        if (request.containsKey("longitude")) spot.setLongitude(new BigDecimal(request.get("longitude").toString()));
        if (request.containsKey("altitude")) spot.setAltitude((String) request.get("altitude"));
        if (request.containsKey("location")) spot.setLocation((String) request.get("location"));

        scenicSpotRepository.save(spot);
        return ResponseEntity.ok(spot);
    }

    /**
     * 删除景点
     */
    @DeleteMapping("/spots/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteSpot(@PathVariable Long id) {
        if (!scenicSpotRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        scenicSpotRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    /**
     * 获取所有资讯
     */
    @GetMapping("/news")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<News>> getAllNews() {
        return ResponseEntity.ok(newsRepository.findAll());
    }

    /**
     * 创建新资讯
     */
    @PostMapping("/news")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createNews(@RequestBody Map<String, Object> request) {
        News news = new News();
        boolean autoTranslate = request.getOrDefault("autoTranslate", true).equals(true);

        String title = (String) request.get("title");
        if (title == null || title.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "标题不能为空"));
        }
        news.setTitle(title);

        if (autoTranslate) {
            String tibetanTitle = translationService.translateOrCreate(title, null, com.tibet.tourism.entity.TibetanDictionary.Type.SENTENCE);
            if (tibetanTitle != null) {
                news.setTitleTibetan(tibetanTitle);
            }
        } else if (request.containsKey("titleTibetan")) {
            news.setTitleTibetan((String) request.get("titleTibetan"));
        }

        String content = (String) request.get("content");
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "内容不能为空"));
        }
        news.setContent(content);

        if (autoTranslate) {
            String tibetanContent = translationService.translateDescription(content);
            if (tibetanContent != null) {
                news.setContentTibetan(tibetanContent);
            }
        } else if (request.containsKey("contentTibetan")) {
            news.setContentTibetan((String) request.get("contentTibetan"));
        }

        if (request.containsKey("category")) {
            try {
                news.setCategory(News.Category.valueOf(((String) request.get("category")).toUpperCase()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "无效的类别"));
            }
        }
        if (request.containsKey("imageUrl")) {
            news.setImageUrl((String) request.get("imageUrl"));
        }
        if (request.containsKey("viewCount")) {
            Object viewCountObj = request.get("viewCount");
            if (viewCountObj instanceof Number) {
                news.setViewCount(((Number) viewCountObj).intValue());
            }
        }

        newsRepository.save(news);
        return ResponseEntity.ok(news);
    }

    /**
     * 更新资讯
     */
    @PutMapping("/news/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateNews(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Optional<News> newsOpt = newsRepository.findById(id);
        if (newsOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        News news = newsOpt.get();
        boolean autoTranslate = request.getOrDefault("autoTranslate", true).equals(true);

        if (request.containsKey("title")) {
            String title = (String) request.get("title");
            news.setTitle(title);
            if (autoTranslate && (news.getTitleTibetan() == null || news.getTitleTibetan().isEmpty())) {
                String tibetanTitle = translationService.translateOrCreate(title, null, com.tibet.tourism.entity.TibetanDictionary.Type.SENTENCE);
                if (tibetanTitle != null) {
                    news.setTitleTibetan(tibetanTitle);
                }
            }
        }

        if (request.containsKey("content")) {
            String content = (String) request.get("content");
            news.setContent(content);
            if (autoTranslate && (news.getContentTibetan() == null || news.getContentTibetan().isEmpty())) {
                String tibetanContent = translationService.translateDescription(content);
                if (tibetanContent != null) {
                    news.setContentTibetan(tibetanContent);
                }
            }
        }

        if (request.containsKey("titleTibetan")) {
            news.setTitleTibetan((String) request.get("titleTibetan"));
        }
        if (request.containsKey("contentTibetan")) {
            news.setContentTibetan((String) request.get("contentTibetan"));
        }
        if (request.containsKey("category")) {
            try {
                news.setCategory(News.Category.valueOf(((String) request.get("category")).toUpperCase()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "无效的类别"));
            }
        }
        if (request.containsKey("imageUrl")) {
            news.setImageUrl((String) request.get("imageUrl"));
        }
        if (request.containsKey("viewCount")) {
            Object viewCountObj = request.get("viewCount");
            if (viewCountObj instanceof Number) {
                news.setViewCount(((Number) viewCountObj).intValue());
            }
        }

        newsRepository.save(news);
        return ResponseEntity.ok(news);
    }

    /**
     * 删除资讯
     */
    @DeleteMapping("/news/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteNews(@PathVariable Long id) {
        if (!newsRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        newsRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    // ========== 轮播图管理 ==========

    @GetMapping("/carousels")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Carousel>> getAllCarousels() {
        return ResponseEntity.ok(carouselRepository.findAll(Sort.by(Sort.Direction.ASC, "sortOrder")));
    }

    @PostMapping("/carousels")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createCarousel(@RequestBody Carousel carousel) {
        if (carousel.getTitle() == null || carousel.getTitle().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "标题不能为空"));
        }
        return ResponseEntity.ok(carouselRepository.save(carousel));
    }

    @PutMapping("/carousels/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateCarousel(@PathVariable Long id, @RequestBody Carousel carousel) {
        Carousel existing = carouselRepository.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        existing.setTitle(carousel.getTitle());
        existing.setSubtitle(carousel.getSubtitle());
        existing.setTag(carousel.getTag());
        existing.setImageUrl(carousel.getImageUrl());
        existing.setLinkUrl(carousel.getLinkUrl());
        existing.setSortOrder(carousel.getSortOrder());
        existing.setActive(carousel.getActive());
        return ResponseEntity.ok(carouselRepository.save(existing));
    }

    @DeleteMapping("/carousels/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteCarousel(@PathVariable Long id) {
        if (!carouselRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        carouselRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    // ========== 酒店管理 ==========

    @GetMapping("/hotels")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Hotel>> getAllHotels() {
        return ResponseEntity.ok(hotelRepository.findAll());
    }

    @PostMapping("/hotels")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createHotel(@RequestBody Map<String, Object> request) {
        Hotel hotel = new Hotel();
        String name = (String) request.get("name");
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "酒店名称不能为空"));
        }
        hotel.setName(name);
        if (request.containsKey("location")) hotel.setLocation((String) request.get("location"));
        if (request.containsKey("phone")) hotel.setPhone((String) request.get("phone"));
        if (request.containsKey("priceRange")) hotel.setPriceRange((String) request.get("priceRange"));
        if (request.containsKey("imageUrl")) hotel.setImageUrl((String) request.get("imageUrl"));
        if (request.containsKey("facilities")) hotel.setFacilities((String) request.get("facilities"));
        if (request.containsKey("rating")) {
            hotel.setRating(new java.math.BigDecimal(request.get("rating").toString()));
        }
        hotelRepository.save(hotel);
        return ResponseEntity.ok(hotel);
    }

    @PutMapping("/hotels/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateHotel(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Hotel hotel = hotelRepository.findById(id).orElse(null);
        if (hotel == null) {
            return ResponseEntity.notFound().build();
        }
        if (request.containsKey("name")) hotel.setName((String) request.get("name"));
        if (request.containsKey("location")) hotel.setLocation((String) request.get("location"));
        if (request.containsKey("phone")) hotel.setPhone((String) request.get("phone"));
        if (request.containsKey("priceRange")) hotel.setPriceRange((String) request.get("priceRange"));
        if (request.containsKey("imageUrl")) hotel.setImageUrl((String) request.get("imageUrl"));
        if (request.containsKey("facilities")) hotel.setFacilities((String) request.get("facilities"));
        if (request.containsKey("rating")) {
            hotel.setRating(new java.math.BigDecimal(request.get("rating").toString()));
        }
        hotelRepository.save(hotel);
        return ResponseEntity.ok(hotel);
    }

    @DeleteMapping("/hotels/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteHotel(@PathVariable Long id) {
        if (!hotelRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        hotelRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    // ========== 线路管理 ==========

    @GetMapping("/routes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TravelRoute>> getAllRoutes() {
        return ResponseEntity.ok(travelRouteRepository.findAll());
    }

    @PostMapping("/routes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createRoute(@RequestBody Map<String, Object> request) {
        TravelRoute route = new TravelRoute();
        String name = (String) request.get("name");
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "线路名称不能为空"));
        }
        route.setName(name);
        if (request.containsKey("nameTibetan")) route.setNameTibetan((String) request.get("nameTibetan"));
        if (request.containsKey("description")) route.setDescription((String) request.get("description"));
        if (request.containsKey("descriptionTibetan")) route.setDescriptionTibetan((String) request.get("descriptionTibetan"));
        if (request.containsKey("days")) route.setDays(((Number) request.get("days")).intValue());
        if (request.containsKey("price")) {
            route.setPrice(new java.math.BigDecimal(request.get("price").toString()));
        }
        if (request.containsKey("difficulty")) {
            route.setDifficulty(TravelRoute.Difficulty.valueOf(((String) request.get("difficulty")).toUpperCase()));
        }
        if (request.containsKey("spotsJson")) route.setSpotsJson((String) request.get("spotsJson"));
        if (request.containsKey("temperature")) route.setTemperature((String) request.get("temperature"));
        if (request.containsKey("geography")) route.setGeography((String) request.get("geography"));
        travelRouteRepository.save(route);
        return ResponseEntity.ok(route);
    }

    @PutMapping("/routes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateRoute(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        TravelRoute route = travelRouteRepository.findById(id).orElse(null);
        if (route == null) {
            return ResponseEntity.notFound().build();
        }
        if (request.containsKey("name")) route.setName((String) request.get("name"));
        if (request.containsKey("nameTibetan")) route.setNameTibetan((String) request.get("nameTibetan"));
        if (request.containsKey("description")) route.setDescription((String) request.get("description"));
        if (request.containsKey("descriptionTibetan")) route.setDescriptionTibetan((String) request.get("descriptionTibetan"));
        if (request.containsKey("days")) route.setDays(((Number) request.get("days")).intValue());
        if (request.containsKey("price")) {
            route.setPrice(new java.math.BigDecimal(request.get("price").toString()));
        }
        if (request.containsKey("difficulty")) {
            route.setDifficulty(TravelRoute.Difficulty.valueOf(((String) request.get("difficulty")).toUpperCase()));
        }
        if (request.containsKey("spotsJson")) route.setSpotsJson((String) request.get("spotsJson"));
        if (request.containsKey("temperature")) route.setTemperature((String) request.get("temperature"));
        if (request.containsKey("geography")) route.setGeography((String) request.get("geography"));
        travelRouteRepository.save(route);
        return ResponseEntity.ok(route);
    }

    @DeleteMapping("/routes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteRoute(@PathVariable Long id) {
        if (!travelRouteRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        travelRouteRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    // ========== 房型管理 ==========

    @GetMapping("/hotels/{hotelId}/room-types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getRoomTypes(@PathVariable Long hotelId) {
        return ResponseEntity.ok(roomTypeRepository.findByHotelIdOrderBySortOrderAsc(hotelId));
    }

    @PostMapping("/hotels/{hotelId}/room-types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createRoomType(@PathVariable Long hotelId, @RequestBody RoomType roomType) {
        hotelRepository.findById(hotelId).ifPresent(roomType::setHotel);
        return ResponseEntity.ok(roomTypeRepository.save(roomType));
    }

    @PutMapping("/room-types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateRoomType(@PathVariable Long id, @RequestBody RoomType roomType) {
        RoomType existing = roomTypeRepository.findById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        existing.setName(roomType.getName());
        existing.setPrice(roomType.getPrice());
        existing.setCapacity(roomType.getCapacity());
        existing.setImageUrl(roomType.getImageUrl());
        existing.setAmenities(roomType.getAmenities());
        existing.setSortOrder(roomType.getSortOrder());
        return ResponseEntity.ok(roomTypeRepository.save(existing));
    }

    @DeleteMapping("/room-types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteRoomType(@PathVariable Long id) {
        if (!roomTypeRepository.existsById(id)) return ResponseEntity.notFound().build();
        roomTypeRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    // ========== 公开房型接口 ==========

    @GetMapping("/public/room-types/{hotelId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getPublicRoomTypes(@PathVariable Long hotelId) {
        return ResponseEntity.ok(roomTypeRepository.findByHotelIdOrderBySortOrderAsc(hotelId));
    }
}
