package com.tibet.tourism.controller;

import com.tibet.tourism.entity.RouteComment;
import com.tibet.tourism.entity.SharedRoute;
import com.tibet.tourism.entity.User;
import com.tibet.tourism.repository.UserRepository;
import com.tibet.tourism.service.SharedRouteService;
import com.tibet.tourism.security.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/routes")
@CrossOrigin(origins = "*")
public class SharedRouteController {

    @Autowired
    private SharedRouteService routeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    // 辅助方法：从请求中获取当前用户ID
    private Long getCurrentUserId(HttpServletRequest request) {
        String jwt = parseJwt(request);
        if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
            String username = jwtUtils.getUserNameFromJwtToken(jwt);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return user.getId();
        }
        throw new RuntimeException("User not authenticated");
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }

    // 分享路线
    @PostMapping("/share")
    public ResponseEntity<?> shareRoute(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            String title = (String) payload.get("title");
            String content = (String) payload.get("content");
            Integer days = (Integer) payload.get("days");
            String budget = (String) payload.get("budget");
            String preference = (String) payload.get("preference");

            SharedRoute route = routeService.shareRoute(userId, title, content, days, budget, preference);
            return ResponseEntity.ok(route);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 获取分享列表
    @GetMapping("/shared")
    public ResponseEntity<?> getSharedRoutes(
            @RequestParam(required = false) Integer days,
            @RequestParam(required = false) String budget,
            @RequestParam(required = false) String preference,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortField) {
        
        Sort sort = Sort.by(Sort.Direction.DESC, sortField);
        Pageable pageable = PageRequest.of(page, size, sort);
        
        Page<SharedRoute> routes = routeService.getRoutes(days, budget, preference, pageable);
        return ResponseEntity.ok(routes);
    }

    // 获取路线详情
    @GetMapping("/shared/{id}")
    public ResponseEntity<?> getRouteDetail(@PathVariable Long id) {
        try {
            SharedRoute route = routeService.getRoute(id);
            return ResponseEntity.ok(route);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 删除路线
    @DeleteMapping("/shared/{id}")
    public ResponseEntity<?> deleteRoute(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            routeService.deleteRoute(id, userId);
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 点赞
    @PostMapping("/shared/{id}/like")
    public ResponseEntity<?> likeRoute(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            boolean success = routeService.likeRoute(id, userId);
            SharedRoute route = routeService.getRoute(id);
            return ResponseEntity.ok(Map.of("liked", success, "likeCount", route.getLikeCount()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 取消点赞
    @DeleteMapping("/shared/{id}/like")
    public ResponseEntity<?> unlikeRoute(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            boolean success = routeService.unlikeRoute(id, userId);
            SharedRoute route = routeService.getRoute(id);
            return ResponseEntity.ok(Map.of("liked", !success, "likeCount", route.getLikeCount()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    // 检查点赞状态
    @GetMapping("/shared/{id}/like-status")
    public ResponseEntity<?> checkLikeStatus(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            boolean isLiked = routeService.isLikedByUser(id, userId);
            return ResponseEntity.ok(Map.of("liked", isLiked));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("liked", false));
        }
    }

    // 添加评论
    @PostMapping("/shared/{id}/comments")
    public ResponseEntity<?> addComment(@PathVariable Long id, @RequestBody Map<String, String> payload, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            String content = payload.get("content");
            RouteComment comment = routeService.addComment(id, userId, content);
            return ResponseEntity.ok(comment);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 获取评论列表
    @GetMapping("/shared/{id}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long id) {
        try {
            List<RouteComment> comments = routeService.getComments(id);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 获取当前用户创建的路线列表
    @GetMapping("/my-routes")
    public ResponseEntity<?> getMyRoutes(HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            List<SharedRoute> routes = routeService.getRoutesByAuthor(user);
            return ResponseEntity.ok(routes);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
}
