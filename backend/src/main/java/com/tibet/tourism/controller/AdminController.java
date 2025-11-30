package com.tibet.tourism.controller;

import com.tibet.tourism.entity.News;
import com.tibet.tourism.entity.ScenicSpot;
import com.tibet.tourism.entity.User;
import com.tibet.tourism.repository.NewsRepository;
import com.tibet.tourism.repository.ScenicSpotRepository;
import com.tibet.tourism.repository.UserRepository;
import com.tibet.tourism.service.PasswordEncryptionService;
import com.tibet.tourism.service.TibetanTranslationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private TibetanTranslationService translationService;

    @Autowired
    private PasswordEncryptionService passwordEncryptionService;

    /**
     * 获取统计信息
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getStats() {
        long userCount = userRepository.count();
        long spotCount = scenicSpotRepository.count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("userCount", userCount);
        stats.put("spotCount", spotCount);
        
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
     * 解密用户密码（仅管理员可用）
     */
    @PostMapping("/users/{id}/decrypt-password")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> decryptPassword(@PathVariable Long id) {
        // 检查服务是否注入成功
        if (passwordEncryptionService == null) {
            System.err.println("=== [AdminController] PasswordEncryptionService 未注入！");
            return ResponseEntity.status(500).body(Map.of(
                    "error", "密码解密服务未初始化，请检查后端配置"
            ));
        }
        
        Optional<User> userOpt = userRepository.findById(id);
        if (userOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        User user = userOpt.get();
        
        // 检查是否有加密密码
        if (user.getEncryptedPassword() == null || user.getEncryptedPassword().isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "message", "该用户没有可解密的密码（可能是旧用户）",
                    "hasEncryptedPassword", false
            ));
        }
        
        try {
            System.out.println("=== [AdminController] 开始解密用户 " + id + " 的密码");
            System.out.println("=== [AdminController] 加密密码长度: " + user.getEncryptedPassword().length());
            
            // 使用 PasswordEncryptionService 解密密码
            String decryptedPassword = passwordEncryptionService.decrypt(user.getEncryptedPassword());
            
            System.out.println("=== [AdminController] 密码解密成功");
            
            return ResponseEntity.ok(Map.of(
                    "password", decryptedPassword,
                    "hasEncryptedPassword", true
            ));
        } catch (Exception e) {
            System.err.println("=== [AdminController] 密码解密失败: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of(
                    "error", "密码解密失败: " + e.getMessage()
            ));
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

        // 更新基本信息
        if (request.containsKey("name")) {
            String name = (String) request.get("name");
            spot.setName(name);
            
            // 如果启用了自动翻译且没有提供藏语名称，尝试自动生成
            if (autoTranslate && (spot.getNameTibetan() == null || spot.getNameTibetan().isEmpty())) {
                String tibetanName = translationService.translateOrCreate(
                        name, 
                        null, 
                        com.tibet.tourism.entity.TibetanDictionary.Type.WORD
                );
                if (tibetanName != null) {
                    spot.setNameTibetan(tibetanName);
                }
            }
        }

        if (request.containsKey("description")) {
            String description = (String) request.get("description");
            spot.setDescription(description);
            
            // 如果启用了自动翻译且没有提供藏语描述，尝试自动生成
            if (autoTranslate && (spot.getDescriptionTibetan() == null || spot.getDescriptionTibetan().isEmpty())) {
                String tibetanDesc = translationService.translateDescription(description);
                if (tibetanDesc != null) {
                    spot.setDescriptionTibetan(tibetanDesc);
                }
            }
        }

        // 如果明确提供了藏语翻译，使用提供的翻译
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

        // 自动生成藏语名称
        if (autoTranslate) {
            String tibetanName = translationService.translateOrCreate(
                    name, 
                    null, 
                    com.tibet.tourism.entity.TibetanDictionary.Type.WORD
            );
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
     * 支持自动生成藏语翻译
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

        // 自动生成藏语标题
        if (autoTranslate) {
            String tibetanTitle = translationService.translateOrCreate(
                    title, 
                    null, 
                    com.tibet.tourism.entity.TibetanDictionary.Type.SENTENCE
            );
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

        // 自动生成藏语内容
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
     * 支持自动生成藏语翻译
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

        // 更新标题
        if (request.containsKey("title")) {
            String title = (String) request.get("title");
            news.setTitle(title);
            
            // 如果启用了自动翻译且没有提供藏语标题，尝试自动生成
            if (autoTranslate && (news.getTitleTibetan() == null || news.getTitleTibetan().isEmpty())) {
                String tibetanTitle = translationService.translateOrCreate(
                        title, 
                        null, 
                        com.tibet.tourism.entity.TibetanDictionary.Type.SENTENCE
                );
                if (tibetanTitle != null) {
                    news.setTitleTibetan(tibetanTitle);
                }
            }
        }

        // 更新内容
        if (request.containsKey("content")) {
            String content = (String) request.get("content");
            news.setContent(content);
            
            // 如果启用了自动翻译且没有提供藏语内容，尝试自动生成
            if (autoTranslate && (news.getContentTibetan() == null || news.getContentTibetan().isEmpty())) {
                String tibetanContent = translationService.translateDescription(content);
                if (tibetanContent != null) {
                    news.setContentTibetan(tibetanContent);
                }
            }
        }

        // 如果明确提供了藏语翻译，使用提供的翻译
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
