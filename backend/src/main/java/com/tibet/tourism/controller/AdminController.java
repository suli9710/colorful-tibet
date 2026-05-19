package com.tibet.tourism.controller;

import com.tibet.tourism.dto.RecommendationEvaluationResponse;
import com.tibet.tourism.entity.Booking;
import com.tibet.tourism.entity.Carousel;
import com.tibet.tourism.entity.Hotel;
import com.tibet.tourism.entity.HotelBooking;
import com.tibet.tourism.entity.News;
import com.tibet.tourism.entity.Comment;
import com.tibet.tourism.entity.RouteComment;
import com.tibet.tourism.entity.RoomType;
import com.tibet.tourism.entity.ScenicSpot;
import com.tibet.tourism.entity.SharedRoute;
import com.tibet.tourism.entity.TravelAnswer;
import com.tibet.tourism.entity.TravelQuestion;
import com.tibet.tourism.entity.TravelRoute;
import com.tibet.tourism.entity.User;
import com.tibet.tourism.repository.BookingRepository;
import com.tibet.tourism.repository.CarouselRepository;
import com.tibet.tourism.repository.CommentLikeRepository;
import com.tibet.tourism.repository.CommentRepository;
import com.tibet.tourism.repository.HotelBookingRepository;
import com.tibet.tourism.repository.HotelRepository;
import com.tibet.tourism.repository.NewsRepository;
import com.tibet.tourism.repository.QuestionLikeRepository;
import com.tibet.tourism.repository.RoomTypeRepository;
import com.tibet.tourism.repository.RouteCommentRepository;
import com.tibet.tourism.repository.RouteLikeRepository;
import com.tibet.tourism.repository.SharedRouteRepository;
import com.tibet.tourism.repository.SpotTagRepository;
import com.tibet.tourism.repository.TravelAnswerRepository;
import com.tibet.tourism.repository.TravelQuestionRepository;
import com.tibet.tourism.repository.UserVisitHistoryRepository;
import com.tibet.tourism.security.InputSanitizer;
import com.tibet.tourism.security.LoginAttemptService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import com.tibet.tourism.repository.ScenicSpotRepository;
import com.tibet.tourism.repository.UserRepository;
import com.tibet.tourism.service.FileStorageService;
import com.tibet.tourism.service.ItemBasedRecommendationService;
import com.tibet.tourism.service.RecommendationEvaluationService;
import com.tibet.tourism.service.TibetanTranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 管理员控制器
 * 提供管理员相关的管理功能
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Set<String> ALLOWED_ROUTE_BUDGETS = Set.of("经济型", "舒适型", "豪华型");
    private static final Set<String> ALLOWED_ROUTE_PREFERENCES = Set.of("自然风光", "人文历史", "深度摄影", "休闲度假");

    public record CarouselRequest(
            @NotBlank(message = "标题不能为空") @Size(max = 80, message = "标题长度不能超过80个字符") String title,
            @Size(max = 160, message = "副标题长度不能超过160个字符") String subtitle,
            @Size(max = 40, message = "标签长度不能超过40个字符") String tag,
            @Size(max = 512, message = "图片地址长度不能超过512个字符") String imageUrl,
            @Size(max = 512, message = "跳转地址长度不能超过512个字符") String linkUrl,
            @Min(value = 0, message = "排序值不能为负数") @Max(value = 10000, message = "排序值过大") Integer sortOrder,
            Boolean active
    ) {
    }

    public record RoomTypeRequest(
            @NotBlank(message = "房型名称不能为空") @Size(max = 80, message = "房型名称长度不能超过80个字符") String name,
            @DecimalMin(value = "0.00", message = "价格不能为负数") BigDecimal price,
            @Min(value = 1, message = "入住人数至少为1") @Max(value = 20, message = "入住人数过大") Integer capacity,
            @Size(max = 512, message = "图片地址长度不能超过512个字符") String imageUrl,
            @Size(max = 500, message = "设施描述长度不能超过500个字符") String amenities,
            @Min(value = 0, message = "排序值不能为负数") @Max(value = 10000, message = "排序值过大") Integer sortOrder
    ) {
    }

    @Autowired
    private ScenicSpotRepository scenicSpotRepository;

    @Autowired
    private SpotTagRepository tagRepository;

    @Autowired
    private NewsRepository newsRepository;

    @Autowired
    private UserRepository userRepository;

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
    private TravelQuestionRepository travelQuestionRepository;

    @Autowired
    private TravelAnswerRepository travelAnswerRepository;

    @Autowired
    private QuestionLikeRepository questionLikeRepository;

    @Autowired
    private UserVisitHistoryRepository userVisitHistoryRepository;

    @Autowired
    private CarouselRepository carouselRepository;

    @Autowired
    private HotelRepository hotelRepository;

    @Autowired
    private RoomTypeRepository roomTypeRepository;

    @Autowired
    private TibetanTranslationService translationService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ItemBasedRecommendationService itemBasedRecommendationService;

    @Autowired
    private RecommendationEvaluationService recommendationEvaluationService;

    @Autowired
    private LoginAttemptService loginAttemptService;

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
        long sharedRouteCount = sharedRouteRepository.count();
        long questionCount = travelQuestionRepository.count();

        // 景点门票订单统计（仅已确认的）
        long scenicBookingCount = bookingRepository.countConfirmed();
        BigDecimal scenicRevenue = bookingRepository.sumConfirmedRevenue();
        if (scenicRevenue == null) scenicRevenue = BigDecimal.ZERO;

        // 酒店预订统计（仅已确认的）
        long hotelBookingCount = hotelBookingRepository.countByStatus(HotelBooking.Status.CONFIRMED);
        BigDecimal hotelRevenue = hotelBookingRepository.sumTotalRevenue();
        if (hotelRevenue == null) hotelRevenue = BigDecimal.ZERO;

        // 合并统计
        long totalOrderCount = scenicBookingCount + hotelBookingCount;
        BigDecimal totalRevenue = scenicRevenue.add(hotelRevenue);

        // 最新混合订单（景点 + 酒店，各取最新的5条）
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
        stats.put("updatedAt", java.time.LocalDateTime.now());

        return ResponseEntity.ok(stats);
    }

    /**
     * 获取所有用户
     */
    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<AdminUserSummary>> getAllUsers(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<AdminUserSummary> users = userRepository.findAll(pageable)
                .map(user -> AdminUserSummary.from(user, loginAttemptService));
        return ResponseEntity.ok(users);
    }

    public record AdminUserSummary(
            Long id,
            String username,
            String nickname,
            User.Role role,
            LocalDateTime createdAt,
            boolean locked,
            long lockRemainingSeconds,
            int failureCount
    ) {
        static AdminUserSummary from(User user, LoginAttemptService service) {
            long remaining = service.remainingLockSeconds(user.getUsername());
            return new AdminUserSummary(
                    user.getId(),
                    user.getUsername(),
                    user.getNickname(),
                    user.getRole(),
                    user.getCreatedAt(),
                    remaining > 0,
                    remaining,
                    service.failureCount(user.getUsername()));
        }
    }

    /**
     * 解除用户登录锁定
     */
    @PostMapping("/users/{id}/unlock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> unlockUser(@PathVariable Long id) {
        User user = userRepository.findById(id).orElse(null);
        if (user == null) {
            return ResponseEntity.notFound().build();
        }
        loginAttemptService.reset(user.getUsername());
        return ResponseEntity.ok(Map.of("message", "已解除登录锁定"));
    }

    @PostMapping("/upload-image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        try {
            String imageUrl = fileStorageService.storeAdminImage(file);
            return ResponseEntity.ok(Map.of("imageUrl", imageUrl));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "上传文件不符合要求"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "上传失败，请稍后重试"));
        }
    }

    @GetMapping("/recommendations/item-similarity/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getItemSimilarityStatus() {
        return ResponseEntity.ok(itemBasedRecommendationService.getSimilarityMatrixStatus());
    }

    @PostMapping("/recommendations/item-similarity/precompute")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> precomputeItemSimilarity() {
        long startTime = System.currentTimeMillis();
        boolean started = itemBasedRecommendationService.precomputeItemSimilarityMatrix();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("success", started);
        body.put("message", started ? "景点相似度矩阵计算完成" : "景点相似度矩阵正在计算中");
        body.put("duration", System.currentTimeMillis() - startTime);
        body.put("status", itemBasedRecommendationService.getSimilarityMatrixStatus());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/recommendations/evaluation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RecommendationEvaluationResponse> evaluateRecommendations(
            @RequestParam(required = false, defaultValue = "10") int k,
            @RequestParam(required = false, defaultValue = "1000") int userLimit,
            @RequestParam(required = false, defaultValue = "false") boolean includeUserResults) {
        return ResponseEntity.ok(recommendationEvaluationService.evaluateItemBasedCf(k, userLimit, includeUserResults));
    }

    /**
     * 更新用户角色
     */
    @PostMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateUserRole(@PathVariable Long id,
                                            @RequestBody Map<String, String> request,
                                            Authentication authentication) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        User user = userOpt.get();
        String role = request.get("role");
        if (role != null) {
            try {
                User.Role requestedRole = User.Role.valueOf(role.toUpperCase());
                String operatorUsername = authentication == null ? "" : authentication.getName();
                boolean operatorIsSuperAdmin = superAdminUsername.equals(operatorUsername);

                if (superAdminUsername.equals(user.getUsername())) {
                    return ResponseEntity.badRequest().body(Map.of("error", "不能修改超级管理员角色"));
                }
                if (operatorUsername.equals(user.getUsername()) && requestedRole != User.Role.ADMIN) {
                    return ResponseEntity.badRequest().body(Map.of("error", "不能取消自己的管理员权限"));
                }
                if (!operatorIsSuperAdmin && (requestedRole == User.Role.ADMIN || user.getRole() == User.Role.ADMIN)) {
                    return ResponseEntity.status(403).body(Map.of("error", "只有超级管理员可以调整管理员角色"));
                }

                user.setRole(requestedRole);
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
            return ResponseEntity.status(403).body(Map.of(
                    "error", "只有超级管理员可以删除用户账户"
            ));
        }

        if (superAdminUsername.equals(targetUser.getUsername())) {
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

        userRepository.delete(targetUser);
        return ResponseEntity.ok(Map.of("message", "用户删除成功"));
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
                .collect(java.util.stream.Collectors.groupingBy(
                        user -> user.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")),
                        java.util.TreeMap::new,
                        java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .map(entry -> Map.<String, Object>of("month", entry.getKey(), "count", entry.getValue()))
                .toList();
    }

    private List<Map<String, Object>> buildNewsPublishTrend() {
        LocalDateTime trendStartAt = YearMonth.now().minusMonths(5).atDay(1).atStartOfDay();
        return newsRepository.findByCreatedAtAfterOrderByCreatedAtAsc(trendStartAt).stream()
                .filter(news -> news.getCreatedAt() != null)
                .collect(java.util.stream.Collectors.groupingBy(
                        news -> news.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")),
                        java.util.TreeMap::new,
                        java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .map(entry -> Map.<String, Object>of("month", entry.getKey(), "count", entry.getValue()))
                .toList();
    }

    /**
     * 获取所有景点
     */
    @GetMapping("/spots")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ScenicSpot>> getAllSpots(
            @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(scenicSpotRepository.findAllWithoutTags(pageable));
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
            String name = InputSanitizer.requiredPlainText((String) request.get("name"), 200, "景点名称");
            spot.setName(name);
            if (autoTranslate && (spot.getNameTibetan() == null || spot.getNameTibetan().isEmpty())) {
                String tibetanName = translationService.translateOrCreate(name, null, com.tibet.tourism.entity.TibetanDictionary.Type.WORD);
                if (tibetanName != null) {
                    spot.setNameTibetan(tibetanName);
                }
            }
        }

        if (request.containsKey("description")) {
            String description = InputSanitizer.optionalTextBlock((String) request.get("description"), 5000, "景点描述");
            spot.setDescription(description);
            if (autoTranslate && description != null && (spot.getDescriptionTibetan() == null || spot.getDescriptionTibetan().isEmpty())) {
                String tibetanDesc = translationService.translateDescription(description);
                if (tibetanDesc != null) {
                    spot.setDescriptionTibetan(tibetanDesc);
                }
            }
        }

        if (request.containsKey("nameTibetan")) {
            spot.setNameTibetan(InputSanitizer.optionalPlainText((String) request.get("nameTibetan"), 200, "藏语名称"));
        }
        if (request.containsKey("descriptionTibetan")) {
            spot.setDescriptionTibetan(InputSanitizer.optionalTextBlock((String) request.get("descriptionTibetan"), 5000, "藏语描述"));
        }
        if (request.containsKey("imageUrl")) {
            spot.setImageUrl(safeImageUrl(request.get("imageUrl"), "景点图片"));
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
            spot.setAltitude(InputSanitizer.optionalPlainText((String) request.get("altitude"), 100, "海拔"));
        }
        if (request.containsKey("location")) {
            spot.setLocation(InputSanitizer.optionalPlainText((String) request.get("location"), 200, "位置"));
        }
        if (request.containsKey("category")) {
            try {
                spot.setCategory(ScenicSpot.Category.valueOf(((String) request.get("category")).toUpperCase()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "无效的类别"));
            }
        }
        if (request.containsKey("num")) spot.setNum(((Number) request.get("num")).intValue());
        if (request.containsKey("openInfo")) spot.setOpenInfo(InputSanitizer.optionalPlainText((String) request.get("openInfo"), 500, "开放信息"));
        if (request.containsKey("entryTime")) spot.setEntryTime(InputSanitizer.optionalPlainText((String) request.get("entryTime"), 200, "入园时间"));
        if (request.containsKey("latitude")) spot.setLatitude(new BigDecimal(request.get("latitude").toString()));
        if (request.containsKey("longitude")) spot.setLongitude(new BigDecimal(request.get("longitude").toString()));

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

        String name = InputSanitizer.requiredPlainText((String) request.get("name"), 200, "景点名称");

        if (autoTranslate) {
            String tibetanName = translationService.translateOrCreate(name, null, com.tibet.tourism.entity.TibetanDictionary.Type.WORD);
            if (tibetanName != null) {
                spot.setNameTibetan(tibetanName);
            }
        } else if (request.containsKey("nameTibetan")) {
            spot.setNameTibetan(InputSanitizer.optionalPlainText((String) request.get("nameTibetan"), 200, "藏语名称"));
        }
        spot.setName(name);

        if (request.containsKey("description")) {
            String description = InputSanitizer.optionalTextBlock((String) request.get("description"), 5000, "景点描述");
            spot.setDescription(description);
            if (autoTranslate && description != null) {
                String tibetanDesc = translationService.translateDescription(description);
                if (tibetanDesc != null) {
                    spot.setDescriptionTibetan(tibetanDesc);
                }
            } else if (request.containsKey("descriptionTibetan")) {
                spot.setDescriptionTibetan(InputSanitizer.optionalTextBlock((String) request.get("descriptionTibetan"), 5000, "藏语描述"));
            }
        }
        if (request.containsKey("imageUrl")) {
            spot.setImageUrl(safeImageUrl(request.get("imageUrl"), "景点图片"));
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
        if (request.containsKey("openInfo")) spot.setOpenInfo(InputSanitizer.optionalPlainText((String) request.get("openInfo"), 500, "开放信息"));
        if (request.containsKey("entryTime")) spot.setEntryTime(InputSanitizer.optionalPlainText((String) request.get("entryTime"), 200, "入园时间"));
        if (request.containsKey("latitude")) spot.setLatitude(new BigDecimal(request.get("latitude").toString()));
        if (request.containsKey("longitude")) spot.setLongitude(new BigDecimal(request.get("longitude").toString()));
        if (request.containsKey("altitude")) spot.setAltitude(InputSanitizer.optionalPlainText((String) request.get("altitude"), 100, "海拔"));
        if (request.containsKey("location")) spot.setLocation(InputSanitizer.optionalPlainText((String) request.get("location"), 200, "位置"));

        scenicSpotRepository.save(spot);
        return ResponseEntity.ok(spot);
    }

    /**
     * 删除景点
     */
    @DeleteMapping("/spots/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> deleteSpot(@PathVariable Long id) {
        if (!scenicSpotRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        commentRepository.findBySpotIdOrderByCreatedAtDesc(id)
                .forEach(comment -> commentLikeRepository.deleteByCommentId(comment.getId()));
        commentRepository.deleteBySpotId(id);
        bookingRepository.deleteBySpotId(id);
        userVisitHistoryRepository.deleteBySpotId(id);
        tagRepository.deleteBySpotId(id);
        scenicSpotRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    /**
     * 获取所有资讯
     */
    @GetMapping("/news")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<News>> getAllNews(
            @PageableDefault(size = 50, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(newsRepository.findAll(pageable));
    }

    /**
     * 创建新资讯
     */
    @PostMapping("/news")
    @PreAuthorize("hasRole('ADMIN')")
    @CacheEvict(value = "newsCache", allEntries = true)
    public ResponseEntity<?> createNews(@RequestBody Map<String, Object> request) {
        News news = new News();
        boolean autoTranslate = request.getOrDefault("autoTranslate", true).equals(true);

        String title = InputSanitizer.requiredPlainText((String) request.get("title"), 200, "标题");

        if (autoTranslate) {
            String tibetanTitle = translationService.translateOrCreate(title, null, com.tibet.tourism.entity.TibetanDictionary.Type.SENTENCE);
            if (tibetanTitle != null) {
                news.setTitleTibetan(tibetanTitle);
            }
        } else if (request.containsKey("titleTibetan")) {
            news.setTitleTibetan(InputSanitizer.optionalPlainText((String) request.get("titleTibetan"), 200, "藏语标题"));
        }
        news.setTitle(title);

        String content = InputSanitizer.requiredTextBlock((String) request.get("content"), 20000, "内容");

        if (autoTranslate) {
            String tibetanContent = translationService.translateDescription(content);
            if (tibetanContent != null) {
                news.setContentTibetan(tibetanContent);
            }
        } else if (request.containsKey("contentTibetan")) {
            news.setContentTibetan(InputSanitizer.optionalTextBlock((String) request.get("contentTibetan"), 20000, "藏语内容"));
        }
        news.setContent(content);

        if (request.containsKey("category")) {
            try {
                news.setCategory(News.Category.valueOf(((String) request.get("category")).toUpperCase()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "无效的类别"));
            }
        }
        if (request.containsKey("imageUrl")) {
            news.setImageUrl(safeImageUrl(request.get("imageUrl"), "资讯图片"));
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
    @CacheEvict(value = "newsCache", allEntries = true)
    public ResponseEntity<?> updateNews(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Optional<News> newsOpt = newsRepository.findById(id);
        if (newsOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        News news = newsOpt.get();
        boolean autoTranslate = request.getOrDefault("autoTranslate", true).equals(true);

        if (request.containsKey("title")) {
            String title = InputSanitizer.requiredPlainText((String) request.get("title"), 200, "标题");
            news.setTitle(title);
            if (autoTranslate && (news.getTitleTibetan() == null || news.getTitleTibetan().isEmpty())) {
                String tibetanTitle = translationService.translateOrCreate(title, null, com.tibet.tourism.entity.TibetanDictionary.Type.SENTENCE);
                if (tibetanTitle != null) {
                    news.setTitleTibetan(tibetanTitle);
                }
            }
        }

        if (request.containsKey("content")) {
            String content = InputSanitizer.requiredTextBlock((String) request.get("content"), 20000, "内容");
            news.setContent(content);
            if (autoTranslate && (news.getContentTibetan() == null || news.getContentTibetan().isEmpty())) {
                String tibetanContent = translationService.translateDescription(content);
                if (tibetanContent != null) {
                    news.setContentTibetan(tibetanContent);
                }
            }
        }

        if (request.containsKey("titleTibetan")) {
            news.setTitleTibetan(InputSanitizer.optionalPlainText((String) request.get("titleTibetan"), 200, "藏语标题"));
        }
        if (request.containsKey("contentTibetan")) {
            news.setContentTibetan(InputSanitizer.optionalTextBlock((String) request.get("contentTibetan"), 20000, "藏语内容"));
        }
        if (request.containsKey("category")) {
            try {
                news.setCategory(News.Category.valueOf(((String) request.get("category")).toUpperCase()));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.badRequest().body(Map.of("error", "无效的类别"));
            }
        }
        if (request.containsKey("imageUrl")) {
            news.setImageUrl(safeImageUrl(request.get("imageUrl"), "资讯图片"));
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
    @CacheEvict(value = "newsCache", allEntries = true)
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
    public ResponseEntity<?> createCarousel(@Valid @RequestBody CarouselRequest request) {
        Carousel carousel = new Carousel();
        applyCarouselRequest(carousel, request);
        return ResponseEntity.ok(carouselRepository.save(carousel));
    }

    @PutMapping("/carousels/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateCarousel(@PathVariable Long id, @Valid @RequestBody CarouselRequest request) {
        Carousel existing = carouselRepository.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }
        applyCarouselRequest(existing, request);
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
    public ResponseEntity<Page<Hotel>> getAllHotels(
            @PageableDefault(size = 50, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        return ResponseEntity.ok(hotelRepository.findAll(pageable));
    }

    @PostMapping("/hotels")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createHotel(@RequestBody Map<String, Object> request) {
        Hotel hotel = new Hotel();
        String name = InputSanitizer.requiredPlainText((String) request.get("name"), 200, "酒店名称");
        hotel.setName(name);
        if (request.containsKey("location")) hotel.setLocation(InputSanitizer.optionalPlainText((String) request.get("location"), 200, "位置"));
        if (request.containsKey("phone")) hotel.setPhone(InputSanitizer.optionalPlainText((String) request.get("phone"), 32, "电话"));
        if (request.containsKey("priceRange")) hotel.setPriceRange(InputSanitizer.optionalPlainText((String) request.get("priceRange"), 100, "价格区间"));
        if (request.containsKey("imageUrl")) hotel.setImageUrl(safeImageUrl(request.get("imageUrl"), "酒店图片"));
        if (request.containsKey("facilities")) hotel.setFacilities(InputSanitizer.optionalPlainText((String) request.get("facilities"), 500, "设施"));
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
        if (request.containsKey("name")) hotel.setName(InputSanitizer.requiredPlainText((String) request.get("name"), 200, "酒店名称"));
        if (request.containsKey("location")) hotel.setLocation(InputSanitizer.optionalPlainText((String) request.get("location"), 200, "位置"));
        if (request.containsKey("phone")) hotel.setPhone(InputSanitizer.optionalPlainText((String) request.get("phone"), 32, "电话"));
        if (request.containsKey("priceRange")) hotel.setPriceRange(InputSanitizer.optionalPlainText((String) request.get("priceRange"), 100, "价格区间"));
        if (request.containsKey("imageUrl")) hotel.setImageUrl(safeImageUrl(request.get("imageUrl"), "酒店图片"));
        if (request.containsKey("facilities")) hotel.setFacilities(InputSanitizer.optionalPlainText((String) request.get("facilities"), 500, "设施"));
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
    public ResponseEntity<Page<Map<String, Object>>> getAllRoutes(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(sharedRouteRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toAdminSharedRoute));
    }

    @PostMapping("/routes")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> createRoute(@RequestBody Map<String, Object> request, Authentication authentication) {
        SharedRoute route = new SharedRoute();
        route.setSourceType(SharedRoute.SourceType.OFFICIAL);
        route.setAuthor(findAuthenticatedUser(authentication).orElse(null));

        ResponseEntity<?> error = applyRouteManagementPayload(route, request, true);
        if (error != null) {
            return error;
        }

        return ResponseEntity.ok(toAdminSharedRoute(sharedRouteRepository.save(route)));
    }

    @PutMapping("/routes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> updateRoute(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Optional<SharedRoute> routeOpt = sharedRouteRepository.findById(id);
        if (routeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        SharedRoute route = routeOpt.get();
        ResponseEntity<?> error = applyRouteManagementPayload(route, request, false);
        if (error != null) {
            return error;
        }

        return ResponseEntity.ok(toAdminSharedRoute(sharedRouteRepository.save(route)));
    }

    @DeleteMapping("/routes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> deleteRoute(@PathVariable Long id) {
        Optional<SharedRoute> routeOpt = sharedRouteRepository.findById(id);
        if (routeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        deleteSharedRouteWithChildren(routeOpt.get());
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    private ResponseEntity<?> applyRouteManagementPayload(SharedRoute route, Map<String, Object> request, boolean creating) {
        String title = firstStringValue(request, "title", "name");
        if (creating || request.containsKey("title") || request.containsKey("name")) {
            if (isBlank(title)) {
                return ResponseEntity.badRequest().body(Map.of("error", "线路名称不能为空"));
            }
            route.setTitle(InputSanitizer.requiredPlainText(title, 200, "线路名称"));
        }

        String content = firstStringValue(request, "content", "description");
        if (creating || request.containsKey("content") || request.containsKey("description")) {
            route.setContent(isBlank(content)
                    ? route.getTitle()
                    : InputSanitizer.requiredTextBlock(content, 12000, "线路内容"));
        }

        if (creating || request.containsKey("days")) {
            Integer days = integerValue(request.get("days"));
            if (days == null || days < 1 || days > 15) {
                return ResponseEntity.badRequest().body(Map.of("error", "天数必须在1到15之间"));
            }
            route.setDays(days);
        }

        if (request.containsKey("budget")) {
            route.setBudget(InputSanitizer.optionalAllowedValue(
                    stringValue(request.get("budget")), ALLOWED_ROUTE_BUDGETS, "预算"));
        } else if (creating && route.getBudget() == null) {
            route.setBudget(resolveBudgetLabel(route.getPrice()));
        }

        if (request.containsKey("preference")) {
            route.setPreference(InputSanitizer.optionalAllowedValue(
                    stringValue(request.get("preference")), ALLOWED_ROUTE_PREFERENCES, "旅行偏好"));
        } else if (creating && route.getPreference() == null) {
            route.setPreference("人文历史");
        }

        if (request.containsKey("price")) {
            BigDecimal price = decimalValue(request.get("price"));
            route.setPrice(price);
            if (price != null && (!request.containsKey("budget") || isBlank(route.getBudget()))) {
                route.setBudget(resolveBudgetLabel(price));
            }
        }

        if (request.containsKey("difficulty")) {
            String difficulty = optionalStringValue(request.get("difficulty"));
            if (difficulty == null) {
                route.setDifficulty(null);
            } else {
                try {
                    route.setDifficulty(TravelRoute.Difficulty.valueOf(difficulty.toUpperCase()).name());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "无效的难度"));
                }
            }
        }

        if (request.containsKey("temperature")) {
            route.setTemperature(InputSanitizer.optionalPlainText(stringValue(request.get("temperature")), 100, "温度说明"));
        }
        if (request.containsKey("geography")) {
            route.setGeography(InputSanitizer.optionalPlainText(stringValue(request.get("geography")), 100, "地理说明"));
        }
        return null;
    }

    private Optional<User> findAuthenticatedUser(Authentication authentication) {
        if (authentication == null || isBlank(authentication.getName())) {
            return Optional.empty();
        }
        return userRepository.findByUsername(authentication.getName());
    }

    private String firstStringValue(Map<String, Object> request, String firstKey, String secondKey) {
        if (request.containsKey(firstKey)) {
            return stringValue(request.get(firstKey));
        }
        if (request.containsKey(secondKey)) {
            return stringValue(request.get(secondKey));
        }
        return null;
    }

    private BigDecimal decimalValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        String string = value.toString();
        if (isBlank(string)) {
            return null;
        }
        try {
            return new BigDecimal(string.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String resolveBudgetLabel(BigDecimal price) {
        if (price == null) {
            return "舒适型";
        }
        if (price.compareTo(new BigDecimal("1000")) < 0) {
            return "经济型";
        }
        if (price.compareTo(new BigDecimal("3000")) < 0) {
            return "舒适型";
        }
        return "豪华型";
    }

    private void deleteSharedRouteWithChildren(SharedRoute route) {
        routeLikeRepository.deleteByRoute(route);
        routeCommentRepository.deleteByRoute(route);
        sharedRouteRepository.delete(route);
    }

    // ========== 社区内容管理 ==========

    @GetMapping("/community/routes")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Map<String, Object>>> getCommunityRoutes(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(sharedRouteRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toAdminSharedRoute));
    }

    @PutMapping("/community/routes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> updateCommunityRoute(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Optional<SharedRoute> routeOpt = sharedRouteRepository.findById(id);
        if (routeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        SharedRoute route = routeOpt.get();
        if (request.containsKey("title")) {
            String title = stringValue(request.get("title"));
            if (isBlank(title)) {
                return ResponseEntity.badRequest().body(Map.of("error", "标题不能为空"));
            }
            route.setTitle(InputSanitizer.requiredPlainText(title, 200, "标题"));
        }
        if (request.containsKey("content")) {
            String content = stringValue(request.get("content"));
            if (isBlank(content)) {
                return ResponseEntity.badRequest().body(Map.of("error", "内容不能为空"));
            }
            route.setContent(InputSanitizer.requiredTextBlock(content, 12000, "内容"));
        }
        if (request.containsKey("days")) {
            Integer days = integerValue(request.get("days"));
            if (days == null || days < 1 || days > 15) {
                return ResponseEntity.badRequest().body(Map.of("error", "天数必须在1到15之间"));
            }
            route.setDays(days);
        }
        if (request.containsKey("budget")) {
            route.setBudget(InputSanitizer.optionalAllowedValue(
                    stringValue(request.get("budget")), ALLOWED_ROUTE_BUDGETS, "预算"));
        }
        if (request.containsKey("preference")) {
            route.setPreference(InputSanitizer.optionalAllowedValue(
                    stringValue(request.get("preference")), ALLOWED_ROUTE_PREFERENCES, "旅行偏好"));
        }

        return ResponseEntity.ok(toAdminSharedRoute(sharedRouteRepository.save(route)));
    }

    @DeleteMapping("/community/routes/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> deleteCommunityRoute(@PathVariable Long id) {
        Optional<SharedRoute> routeOpt = sharedRouteRepository.findById(id);
        if (routeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        deleteSharedRouteWithChildren(routeOpt.get());
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    @GetMapping("/community/comments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Map<String, Object>>> getCommunityComments(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(routeCommentRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toAdminRouteComment));
    }

    @PutMapping("/community/comments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> updateCommunityComment(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Optional<RouteComment> commentOpt = routeCommentRepository.findById(id);
        if (commentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        RouteComment comment = commentOpt.get();
        if (request.containsKey("content")) {
            String content = stringValue(request.get("content"));
            if (isBlank(content)) {
                return ResponseEntity.badRequest().body(Map.of("error", "内容不能为空"));
            }
            comment.setContent(InputSanitizer.requiredTextBlock(content, 1000, "内容"));
        }

        return ResponseEntity.ok(toAdminRouteComment(routeCommentRepository.save(comment)));
    }

    @DeleteMapping("/community/comments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> deleteCommunityComment(@PathVariable Long id) {
        Optional<RouteComment> commentOpt = routeCommentRepository.findById(id);
        if (commentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        RouteComment comment = commentOpt.get();
        SharedRoute route = comment.getRoute();
        routeCommentRepository.delete(comment);
        if (route != null) {
            route.decrementCommentCount();
            sharedRouteRepository.save(route);
        }
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    @GetMapping("/community/spot-comments")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Map<String, Object>>> getCommunitySpotComments(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(commentRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toAdminSpotComment));
    }

    @PutMapping("/community/spot-comments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> updateCommunitySpotComment(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Optional<Comment> commentOpt = commentRepository.findById(id);
        if (commentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Comment comment = commentOpt.get();
        if (request.containsKey("content")) {
            String content = stringValue(request.get("content"));
            if (isBlank(content)) {
                return ResponseEntity.badRequest().body(Map.of("error", "内容不能为空"));
            }
            comment.setContent(InputSanitizer.requiredTextBlock(content, 1000, "内容"));
        }
        if (request.containsKey("rating") && request.get("rating") != null) {
            int rating = Integer.parseInt(String.valueOf(request.get("rating")));
            if (rating < 1 || rating > 5) {
                return ResponseEntity.badRequest().body(Map.of("error", "评分必须在1到5之间"));
            }
            comment.setRating(rating);
        }

        return ResponseEntity.ok(toAdminSpotComment(commentRepository.save(comment)));
    }

    @DeleteMapping("/community/spot-comments/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> deleteCommunitySpotComment(@PathVariable Long id) {
        Optional<Comment> commentOpt = commentRepository.findById(id);
        if (commentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        commentLikeRepository.deleteByCommentId(id);
        commentRepository.delete(commentOpt.get());
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    @GetMapping("/community/questions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Map<String, Object>>> getCommunityQuestions(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(travelQuestionRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toAdminQuestion));
    }

    @PutMapping("/community/questions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> updateCommunityQuestion(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Optional<TravelQuestion> questionOpt = travelQuestionRepository.findById(id);
        if (questionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TravelQuestion question = questionOpt.get();
        if (request.containsKey("title")) {
            String title = stringValue(request.get("title"));
            if (isBlank(title)) {
                return ResponseEntity.badRequest().body(Map.of("error", "标题不能为空"));
            }
            question.setTitle(InputSanitizer.requiredPlainText(title, 200, "标题"));
        }
        if (request.containsKey("content")) {
            String content = stringValue(request.get("content"));
            if (isBlank(content)) {
                return ResponseEntity.badRequest().body(Map.of("error", "内容不能为空"));
            }
            question.setContent(InputSanitizer.requiredTextBlock(content, 4000, "内容"));
        }
        if (request.containsKey("tags")) {
            question.setTags(InputSanitizer.optionalTags(stringValue(request.get("tags")), 500));
        }
        if (request.containsKey("isResolved")) {
            question.setIsResolved(booleanValue(request.get("isResolved")));
        }

        return ResponseEntity.ok(toAdminQuestion(travelQuestionRepository.save(question)));
    }

    @DeleteMapping("/community/questions/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> deleteCommunityQuestion(@PathVariable Long id) {
        Optional<TravelQuestion> questionOpt = travelQuestionRepository.findById(id);
        if (questionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TravelQuestion question = questionOpt.get();
        questionLikeRepository.deleteByQuestion(question);
        travelAnswerRepository.deleteByQuestion(question);
        travelQuestionRepository.delete(question);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    @GetMapping("/community/answers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<Map<String, Object>>> getCommunityAnswers(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(travelAnswerRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toAdminAnswer));
    }

    @PutMapping("/community/answers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> updateCommunityAnswer(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        Optional<TravelAnswer> answerOpt = travelAnswerRepository.findById(id);
        if (answerOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TravelAnswer answer = answerOpt.get();
        if (request.containsKey("content")) {
            String content = stringValue(request.get("content"));
            if (isBlank(content)) {
                return ResponseEntity.badRequest().body(Map.of("error", "内容不能为空"));
            }
            answer.setContent(InputSanitizer.requiredTextBlock(content, 4000, "内容"));
        }
        if (request.containsKey("isAccepted")) {
            Boolean accepted = booleanValue(request.get("isAccepted"));
            answer.setIsAccepted(accepted);

            TravelQuestion question = answer.getQuestion();
            if (question != null) {
                if (Boolean.TRUE.equals(accepted)) {
                    question.setIsResolved(true);
                } else if (!hasAcceptedAnswerExcept(question, answer.getId())) {
                    question.setIsResolved(false);
                }
                travelQuestionRepository.save(question);
            }
        }

        return ResponseEntity.ok(toAdminAnswer(travelAnswerRepository.save(answer)));
    }

    @DeleteMapping("/community/answers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<?> deleteCommunityAnswer(@PathVariable Long id) {
        Optional<TravelAnswer> answerOpt = travelAnswerRepository.findById(id);
        if (answerOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TravelAnswer answer = answerOpt.get();
        TravelQuestion question = answer.getQuestion();
        boolean wasAccepted = Boolean.TRUE.equals(answer.getIsAccepted());
        travelAnswerRepository.delete(answer);

        if (question != null) {
            question.decrementAnswerCount();
            if (wasAccepted && !hasAcceptedAnswerExcept(question, answer.getId())) {
                question.setIsResolved(false);
            }
            travelQuestionRepository.save(question);
        }
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    private Map<String, Object> toAdminSharedRoute(SharedRoute route) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", route.getId());
        data.put("title", route.getTitle());
        data.put("name", route.getTitle());
        data.put("content", route.getContent());
        data.put("description", route.getContent());
        data.put("days", route.getDays());
        data.put("budget", route.getBudget());
        data.put("preference", route.getPreference());
        data.put("sourceType", route.getSourceType());
        data.put("sourceRouteId", route.getSourceRouteId());
        data.put("price", route.getPrice());
        data.put("difficulty", route.getDifficulty());
        data.put("temperature", route.getTemperature());
        data.put("geography", route.getGeography());
        data.put("viewCount", route.getViewCount());
        data.put("likeCount", route.getLikeCount());
        data.put("commentCount", route.getCommentCount());
        data.put("createdAt", route.getCreatedAt());
        data.put("updatedAt", route.getUpdatedAt());
        data.put("author", toUserSummary(route.getAuthor()));
        return data;
    }

    private Map<String, Object> toAdminRouteComment(RouteComment comment) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", comment.getId());
        data.put("content", comment.getContent());
        data.put("createdAt", comment.getCreatedAt());
        data.put("user", toUserSummary(comment.getUser()));
        if (comment.getRoute() != null) {
            data.put("routeId", comment.getRoute().getId());
            data.put("routeTitle", comment.getRoute().getTitle());
        }
        return data;
    }

    private Map<String, Object> toAdminSpotComment(Comment comment) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", comment.getId());
        data.put("content", comment.getContent());
        data.put("rating", comment.getRating());
        data.put("imageUrl", comment.getImageUrl());
        data.put("likeCount", comment.getLikeCount());
        data.put("createdAt", comment.getCreatedAt());
        data.put("user", toUserSummary(comment.getUser()));
        if (comment.getSpot() != null) {
            data.put("spotId", comment.getSpot().getId());
            data.put("spotName", comment.getSpot().getName());
        }
        return data;
    }

    private Map<String, Object> toAdminQuestion(TravelQuestion question) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", question.getId());
        data.put("title", question.getTitle());
        data.put("content", question.getContent());
        data.put("tags", question.getTags());
        data.put("viewCount", question.getViewCount());
        data.put("answerCount", question.getAnswerCount());
        data.put("likeCount", question.getLikeCount());
        data.put("isResolved", question.getIsResolved());
        data.put("createdAt", question.getCreatedAt());
        data.put("updatedAt", question.getUpdatedAt());
        data.put("author", toUserSummary(question.getAuthor()));
        return data;
    }

    private Map<String, Object> toAdminAnswer(TravelAnswer answer) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", answer.getId());
        data.put("content", answer.getContent());
        data.put("likeCount", answer.getLikeCount());
        data.put("isAccepted", answer.getIsAccepted());
        data.put("createdAt", answer.getCreatedAt());
        data.put("user", toUserSummary(answer.getUser()));
        if (answer.getQuestion() != null) {
            data.put("questionId", answer.getQuestion().getId());
            data.put("questionTitle", answer.getQuestion().getTitle());
        }
        return data;
    }

    private Map<String, Object> toUserSummary(User user) {
        Map<String, Object> data = new HashMap<>();
        if (user == null) {
            return data;
        }
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("role", user.getRole());
        return data;
    }

    private boolean hasAcceptedAnswerExcept(TravelQuestion question, Long ignoredAnswerId) {
        return travelAnswerRepository.findByQuestionOrderByIsAcceptedDescLikeCountDescCreatedAtAsc(question)
                .stream()
                .anyMatch(answer -> Boolean.TRUE.equals(answer.getIsAccepted())
                        && (ignoredAnswerId == null || !ignoredAnswerId.equals(answer.getId())));
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String stringValue(Object value) {
        return value == null ? null : value.toString();
    }

    private String optionalStringValue(Object value) {
        String string = stringValue(value);
        return isBlank(string) ? null : string.trim();
    }

    private String safeImageUrl(Object value, String fieldName) {
        return InputSanitizer.optionalPublicImageUrl(stringValue(value), fieldName);
    }

    private void applyCarouselRequest(Carousel carousel, CarouselRequest request) {
        carousel.setTitle(InputSanitizer.requiredPlainText(request.title(), 80, "轮播图标题"));
        carousel.setSubtitle(InputSanitizer.optionalPlainText(request.subtitle(), 160, "轮播图副标题"));
        carousel.setTag(InputSanitizer.optionalPlainText(request.tag(), 40, "轮播图标签"));
        carousel.setImageUrl(safeImageUrl(request.imageUrl(), "轮播图图片"));
        carousel.setLinkUrl(InputSanitizer.optionalSafeLinkUrl(request.linkUrl(), "轮播图跳转地址"));
        carousel.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
        carousel.setActive(request.active() == null || request.active());
    }

    private void applyRoomTypeRequest(RoomType roomType, RoomTypeRequest request) {
        roomType.setName(InputSanitizer.requiredPlainText(request.name(), 80, "房型名称"));
        roomType.setPrice(request.price() == null ? BigDecimal.ZERO : request.price());
        roomType.setCapacity(request.capacity() == null ? 1 : request.capacity());
        roomType.setImageUrl(safeImageUrl(request.imageUrl(), "房型图片"));
        roomType.setAmenities(InputSanitizer.optionalPlainText(request.amenities(), 500, "房型设施"));
        roomType.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());
    }

    private Integer integerValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string && !string.trim().isEmpty()) {
            try {
                return Integer.parseInt(string.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private Boolean booleanValue(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof String string) {
            return Boolean.parseBoolean(string);
        }
        return false;
    }

    // ========== 房型管理 ==========

    @GetMapping("/hotels/{hotelId}/room-types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> getRoomTypes(@PathVariable Long hotelId) {
        return ResponseEntity.ok(roomTypeRepository.findByHotelIdOrderBySortOrderAsc(hotelId));
    }

    @PostMapping("/hotels/{hotelId}/room-types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> createRoomType(@PathVariable Long hotelId, @Valid @RequestBody RoomTypeRequest request) {
        Hotel hotel = hotelRepository.findById(hotelId).orElse(null);
        if (hotel == null) {
            return ResponseEntity.notFound().build();
        }
        RoomType roomType = new RoomType();
        roomType.setHotel(hotel);
        applyRoomTypeRequest(roomType, request);
        return ResponseEntity.ok(roomTypeRepository.save(roomType));
    }

    @PutMapping("/room-types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> updateRoomType(@PathVariable Long id, @Valid @RequestBody RoomTypeRequest request) {
        RoomType existing = roomTypeRepository.findById(id).orElse(null);
        if (existing == null) return ResponseEntity.notFound().build();
        applyRoomTypeRequest(existing, request);
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
