package com.tibet.tourism.modules.community.web;
import com.tibet.tourism.common.api.PageResponse;
import com.tibet.tourism.common.error.AuthenticationRequiredException;
import com.tibet.tourism.common.error.BusinessException;
import com.tibet.tourism.common.error.ResourceNotFoundException;
import com.tibet.tourism.common.error.UnauthorizedActionException;
import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.common.security.SensitiveLogSanitizer;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.community.application.SharedRouteService;
import com.tibet.tourism.modules.community.domain.RouteComment;
import com.tibet.tourism.modules.community.domain.SharedRoute;
import com.tibet.tourism.modules.community.web.dto.RouteCommentResponse;
import com.tibet.tourism.modules.community.web.dto.ShareRouteRequest;
import com.tibet.tourism.modules.community.web.dto.SharedRouteResponse;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/routes")
public class SharedRouteController {

    private static final Logger logger = LoggerFactory.getLogger(SharedRouteController.class);
    private static final String ERROR_AUTHENTICATION_REQUIRED = "Authentication required";
    private static final String ERROR_PERMISSION_DENIED = "Permission denied";
    private static final String ERROR_NOT_FOUND = "Resource not found";
    private static final String ERROR_INVALID_REQUEST = "Invalid request";

    private static final Set<String> ALLOWED_ROUTE_SORT_FIELDS = Set.of(
            "createdAt", "updatedAt", "viewCount", "likeCount", "commentCount", "days");

    @Autowired
    private SharedRouteService routeService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtAuthSupport jwtAuthSupport;

    // 辅助方法：从请求中获取当前用户ID
    private long getCurrentUserId(HttpServletRequest request) {
        try {
            return jwtAuthSupport.resolveCurrentUserId(request);
        } catch (UsernameNotFoundException e) {
            throw new ResourceNotFoundException("User not found");
        } catch (IllegalStateException e) {
            throw new AuthenticationRequiredException("User not authenticated");
        }
    }

    private Long getOptionalCurrentUserId(HttpServletRequest request) {
        try {
            Optional<User> currentUser = jwtAuthSupport.resolveOptionalCurrentUser(request);
            return currentUser == null ? null : currentUser.map(User::getId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private ResponseEntity<Map<String, String>> safeBadRequest(Exception e) {
        logger.warn("Shared route request failed: {}", SensitiveLogSanitizer.exceptionSummary(e));
        if (e instanceof AuthenticationRequiredException) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ERROR_AUTHENTICATION_REQUIRED));
        }
        if (e instanceof UnauthorizedActionException || e instanceof SecurityException) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ERROR_PERMISSION_DENIED));
        }
        if (e instanceof ResourceNotFoundException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ERROR_NOT_FOUND));
        }
        if (e instanceof BusinessException || e instanceof IllegalArgumentException) {
            return ResponseEntity.badRequest().body(Map.of("error", ERROR_INVALID_REQUEST));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "请求处理失败，请检查输入后重试"));
    }

    // 分享路线
    @PostMapping("/share")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> shareRoute(@Valid @RequestBody ShareRouteRequest dto, HttpServletRequest request) {
        try {
            long userId = getCurrentUserId(request);
            SharedRoute route = routeService.shareRoute(
                    userId,
                    dto.getTitle(),
                    dto.getContent(),
                    dto.getDays(),
                    dto.getBudget(),
                    dto.getPreference()
            );
            return ResponseEntity.ok(SharedRouteResponse.fromEntity(route, userId));
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
            @RequestParam(defaultValue = "createdAt") String sortField,
            HttpServletRequest request) {

        String safeSortField = InputSanitizer.safeSortField(sortField, ALLOWED_ROUTE_SORT_FIELDS, "createdAt");
        Sort sort = Sort.by(Sort.Direction.DESC, safeSortField);
        Pageable pageable = PageRequest.of(
                InputSanitizer.normalizePage(page),
                InputSanitizer.normalizePageSize(size, 10, 50),
                sort);
        
        Long currentUserId = getOptionalCurrentUserId(request);
        Page<SharedRouteResponse> routes = routeService.getRoutes(days, budget, preference, pageable)
                .map(route -> SharedRouteResponse.fromEntity(route, currentUserId));
        return ResponseEntity.ok(PageResponse.from(routes));
    }

    // 获取路线详情
    @GetMapping("/shared/{id}")
    public ResponseEntity<?> getRouteDetail(@PathVariable Long id, HttpServletRequest request) {
        try {
            SharedRoute route = routeService.getRoute(id);
            return ResponseEntity.ok(SharedRouteResponse.fromEntity(route, getOptionalCurrentUserId(request)));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 删除路线
    @DeleteMapping("/shared/{id}")
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> checkLikeStatus(@PathVariable Long id, HttpServletRequest request) {
        try {
            Optional<User> currentUser = jwtAuthSupport.resolveOptionalCurrentUser(request);
            boolean isLiked = currentUser
                    .map(user -> routeService.isLikedByUser(id, user.getId()))
                    .orElse(false);
            return ResponseEntity.ok(Map.of("liked", isLiked));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("liked", false));
        }
    }

    // 添加评论
    @PostMapping("/shared/{id}/comments")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> addComment(@PathVariable Long id, @RequestBody Map<String, String> payload, HttpServletRequest request) {
        try {
            Long userId = getCurrentUserId(request);
            String content = payload.get("content");
            RouteComment comment = routeService.addComment(id, userId, content);
            return ResponseEntity.ok(RouteCommentResponse.fromEntity(comment, userId));
        } catch (Exception e) {
            return safeBadRequest(e);
        }
    }

    // 获取评论列表
    @GetMapping("/shared/{id}/comments")
    public ResponseEntity<?> getComments(@PathVariable Long id, HttpServletRequest request) {
        try {
            Long currentUserId = getOptionalCurrentUserId(request);
            List<RouteCommentResponse> comments = routeService.getComments(id).stream()
                    .map(comment -> RouteCommentResponse.fromEntity(comment, currentUserId))
                    .toList();
            return ResponseEntity.ok(comments);
        } catch (Exception e) {
            return safeBadRequest(e);
        }
    }

    // 删除自己的评论
    @DeleteMapping("/shared/{id}/comments/{commentId}")
    @PreAuthorize("isAuthenticated()")
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
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getMyRoutes(HttpServletRequest request) {
        try {
            User user = userRepository.findById(getCurrentUserId(request))
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            List<SharedRouteResponse> routes = routeService.getRoutesByAuthor(user).stream()
                    .map(route -> SharedRouteResponse.fromEntity(route, user.getId()))
                    .toList();
            return ResponseEntity.ok(routes);
        } catch (Exception e) {
            return safeBadRequest(e);
        }
    }
}
