package com.tibet.tourism.controller;

import com.tibet.tourism.entity.RouteComment;
import com.tibet.tourism.entity.SharedRoute;
import com.tibet.tourism.entity.User;
import com.tibet.tourism.exception.AuthenticationRequiredException;
import com.tibet.tourism.exception.BusinessException;
import com.tibet.tourism.exception.ResourceNotFoundException;
import com.tibet.tourism.exception.UnauthorizedActionException;
import com.tibet.tourism.repository.UserRepository;
import com.tibet.tourism.security.CookieAuthConstants;
import com.tibet.tourism.security.InputSanitizer;
import com.tibet.tourism.service.SharedRouteService;
import com.tibet.tourism.security.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.WebUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/routes")
public class SharedRouteController {

    private static final Logger logger = LoggerFactory.getLogger(SharedRouteController.class);

    private static final Set<String> ALLOWED_ROUTE_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "viewCount", "likeCount", "commentCount", "days");

    @Autowired
    private SharedRouteService routeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    // 辅助方法：从请求中获取当前用户ID
    private long getCurrentUserId(HttpServletRequest request) {
        String jwt = parseJwt(request);
        if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
            String username = jwtUtils.getUserNameFromJwtToken(jwt);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            return user.getId();
        }
        throw new AuthenticationRequiredException("User not authenticated");
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        var authCookie = WebUtils.getCookie(request, CookieAuthConstants.AUTH_COOKIE_NAME);
        return authCookie == null ? null : authCookie.getValue();
    }

    private ResponseEntity<Map<String, String>> safeBadRequest(Exception e) {
        logger.warn("Shared route request failed: {}", e.getMessage());
        if (e instanceof AuthenticationRequiredException) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
        if (e instanceof UnauthorizedActionException || e instanceof SecurityException) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
        if (e instanceof ResourceNotFoundException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
        if (e instanceof BusinessException || e instanceof IllegalArgumentException) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "请求处理失败，请检查输入后重试"));
    }

    // 分享路线
    @PostMapping("/share")
    public ResponseEntity<?> shareRoute(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        try {
            return ResponseEntity.ok(routeService.shareRoute(
                    getCurrentUserId(request),
                    (String) payload.get("title"),
                    (String) payload.get("content"),
                    (Integer) payload.get("days"),
                    (String) payload.get("budget"),
                    (String) payload.get("preference")
            ));
        } catch (Exception e) {
            return safeBadRequest(e);
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

        String safeSortField = InputSanitizer.safeSortField(sortField, ALLOWED_ROUTE_SORT_FIELDS, "createdAt");
        Sort sort = Sort.by(Sort.Direction.DESC, safeSortField);
        Pageable pageable = PageRequest.of(
                InputSanitizer.normalizePage(page),
                InputSanitizer.normalizePageSize(size, 10, 50),
                sort);
        
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
            return safeBadRequest(e);
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
            return safeBadRequest(e);
        }
    }

    // 取消点赞
    @DeleteMapping("/shared/{id}/like")
    public ResponseEntity<?> unlikeRoute(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            boolean success = routeService.unlikeRoute(id, userId);
            SharedRoute route = routeService.getRoute(id);
            return ResponseEntity.ok(Map.of("liked", !success, "likeCount", route.getLikeCount() == null ? 0L : route.getLikeCount()));
        } catch (Exception e) {
            return safeBadRequest(e);
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
            return safeBadRequest(e);
        }
    }

    // 获取评论列表
    @GetMapping("/shared/{id}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long id) {
        try {
            List<RouteComment> comments = routeService.getComments(id);
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            return safeBadRequest(e);
        }
    }

    // 删除自己的评论
    @DeleteMapping("/shared/{id}/comments/{commentId}")
    public ResponseEntity<?> deleteComment(@PathVariable Long id, @PathVariable Long commentId, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            routeService.deleteComment(id, commentId, userId);
            return ResponseEntity.noContent().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "无权操作该资源"));
        } catch (Exception e) {
            return safeBadRequest(e);
        }
    }

    // 获取当前用户创建的路线列表
    @GetMapping("/my-routes")
    public ResponseEntity<?> getMyRoutes(HttpServletRequest request) {
        try {
            User user = userRepository.findById(getCurrentUserId(request))
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            List<SharedRoute> routes = routeService.getRoutesByAuthor(user);
            return ResponseEntity.ok(routes);
        } catch (Exception e) {
            return safeBadRequest(e);
        }
    }
}
