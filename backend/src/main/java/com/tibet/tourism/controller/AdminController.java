package com.tibet.tourism.controller;

import com.tibet.tourism.entity.AuditLog;
import com.tibet.tourism.entity.Booking;
import com.tibet.tourism.entity.HotelBooking;
import com.tibet.tourism.entity.News;
import com.tibet.tourism.entity.ScenicSpot;
import com.tibet.tourism.entity.User;
import com.tibet.tourism.repository.AuditLogRepository;
import com.tibet.tourism.repository.BookingRepository;
import com.tibet.tourism.repository.CommentLikeRepository;
import com.tibet.tourism.repository.CommentRepository;
import com.tibet.tourism.repository.HotelBookingRepository;
import com.tibet.tourism.repository.NewsRepository;
import com.tibet.tourism.repository.RouteCommentRepository;
import com.tibet.tourism.repository.RouteLikeRepository;
import com.tibet.tourism.repository.SharedRouteRepository;
import com.tibet.tourism.repository.UserVisitHistoryRepository;
import org.springframework.data.domain.Sort;
import com.tibet.tourism.repository.ScenicSpotRepository;
import com.tibet.tourism.repository.UserRepository;
import com.tibet.tourism.service.PasswordEncryptionService;
import com.tibet.tourism.service.TibetanTranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
@CrossOrigin(origins = "*")
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
    private TibetanTranslationService translationService;

    @Autowired
    private PasswordEncryptionService passwordEncryptionService;

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
        stats.put("orderCount", totalOrderCount);
        stats.put("totalRevenue", totalRevenue);
        stats.put("recentBookings", recentScenicBookings);
        stats.put("recentHotelBookings", recentHotelBookings);

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

    /**
     * 解密用户密码（仅超级管理员可用）
     */
    @PostMapping("/users/{id}/decrypt-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> decryptPassword(@PathVariable Long id, Authentication authentication) {
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        User user = userOpt.get();
        String operatorUsername = authentication == null ? "anonymous" : authentication.getName();

        if (authentication == null || !superAdminUsername.equals(operatorUsername)) {
            recordAudit(AuditLog.Action.DECRYPT_PASSWORD_DENIED, operatorUsername, user, "仅超级管理员可查看用户密码");
            return ResponseEntity.status(403).body(Map.of(
                    "error", "只有超级管理员可以查看用户密码"
            ));
        }

        if (user.getEncryptedPassword() == null || user.getEncryptedPassword().isEmpty()) {
            recordAudit(AuditLog.Action.DECRYPT_PASSWORD_DENIED, operatorUsername, user, "目标用户没有可解密的密码");
            return ResponseEntity.ok(Map.of(
                    "message", "该用户没有可解密的密码（可能是旧用户）",
                    "hasEncryptedPassword", false
            ));
        }

        try {
            String decryptedPassword = passwordEncryptionService.decrypt(user.getEncryptedPassword());
            recordAudit(AuditLog.Action.DECRYPT_PASSWORD, operatorUsername, user, "成功解密用户密码");
            return ResponseEntity.ok(Map.of(
                    "password", decryptedPassword,
                    "hasEncryptedPassword", true
            ));
        } catch (Exception e) {
            recordAudit(AuditLog.Action.DECRYPT_PASSWORD_DENIED, operatorUsername, user, "密码解密失败: " + e.getMessage());
            return ResponseEntity.status(500).body(Map.of(
                    "error", "密码解密失败: " + e.getMessage()
            ));
        }
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
}
