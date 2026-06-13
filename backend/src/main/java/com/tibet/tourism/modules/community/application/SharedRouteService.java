package com.tibet.tourism.modules.community.application;
import com.tibet.tourism.common.error.BusinessException;
import com.tibet.tourism.common.error.ResourceNotFoundException;
import com.tibet.tourism.common.error.UnauthorizedActionException;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.community.domain.RouteComment;
import com.tibet.tourism.modules.community.domain.SharedRoute;
import com.tibet.tourism.modules.community.infra.CommentRepository;
import com.tibet.tourism.modules.community.infra.RouteCommentRepository;
import com.tibet.tourism.modules.community.infra.RouteLikeRepository;
import com.tibet.tourism.modules.community.infra.SharedRouteRepository;
import com.tibet.tourism.modules.community.infra.SharedRouteSummaryRow;
import com.tibet.tourism.modules.community.web.dto.PublicUserResponse;
import com.tibet.tourism.modules.community.web.dto.SharedRouteSummaryResponse;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SharedRouteService {

    private static final Set<String> ALLOWED_BUDGETS = Set.of("经济型", "舒适型", "豪华型");
    private static final Set<String> ALLOWED_PREFERENCES = Set.of("自然风光", "人文历史", "深度摄影", "休闲度假");
    private static final Map<String, String> BUDGET_ALIASES = Map.of(
            "economy", "经济型",
            "comfort", "舒适型",
            "luxury", "豪华型",
            "经济型", "经济型",
            "舒适型", "舒适型",
            "豪华型", "豪华型");
    private static final Map<String, String> PREFERENCE_ALIASES = Map.of(
            "natural", "自然风光",
            "cultural", "人文历史",
            "photography", "深度摄影",
            "relaxation", "休闲度假",
            "自然风光", "自然风光",
            "人文历史", "人文历史",
            "深度摄影", "深度摄影",
            "休闲度假", "休闲度假");

    @Autowired
    private SharedRouteRepository routeRepository;

    @Autowired
    private RouteCommentRepository commentRepository;

    @Autowired
    private RouteLikeRepository likeRepository;

    @Autowired
    private UserRepository userRepository;

    public record LikeResult(boolean liked, int likeCount) {}

    // 分享路线
    @Transactional
    public SharedRoute shareRoute(Long userId, String title, String content, Integer days, String budget, String preference) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        SharedRoute route = new SharedRoute();
        route.setAuthor(user);
        route.setTitle(InputSanitizer.requiredPlainText(title, 200, "路线标题"));
        route.setContent(InputSanitizer.requiredTextBlock(content, 12000, "路线内容"));
        route.setDays(validateDays(days));
        route.setBudget(normalizeBudget(budget));
        route.setPreference(normalizePreference(preference));

        return routeRepository.save(route);
    }

    // 获取路线列表（带筛选）
    @Transactional(readOnly = true)
    public Page<SharedRouteSummaryResponse> getRoutes(Integer days, String budget, String preference, Pageable pageable) {
        return getRoutes(days, budget, preference, pageable, null);
    }

    @Transactional(readOnly = true)
    public Page<SharedRouteSummaryResponse> getRoutes(
            Integer days,
            String budget,
            String preference,
            Pageable pageable,
            Long currentUserId) {
        String safeBudget = normalizeBudget(budget);
        String safePreference = normalizePreference(preference);
        return routeRepository.findSummaries(days, safeBudget, safePreference, pageable)
                .map(row -> toSummaryResponse(row, currentUserId));
    }

    // 获取路线详情
    @Transactional
    public SharedRoute getRoute(Long id) {
        // 原子增加浏览量，避免乐观锁冲突
        int updated = routeRepository.incrementViewCount(id);
        if (updated == 0) {
            throw new ResourceNotFoundException("Route not found");
        }
        return routeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));
    }

    // 删除路线
    @Transactional
    public void deleteRoute(Long routeId, Long userId) {
        SharedRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));

        if (route.getAuthor() == null || !route.getAuthor().getId().equals(userId)) {
            throw new UnauthorizedActionException("Unauthorized: You can only delete your own routes");
        }

        likeRepository.deleteByRouteId(routeId);
        commentRepository.deleteByRouteId(routeId);
        routeRepository.delete(route);
    }

    // 点赞路线
    @Transactional
    public LikeResult likeRoute(Long routeId, Long userId) {
        SharedRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        int inserted = likeRepository.insertIgnore(route.getId(), user.getId());
        if (inserted > 0) {
            routeRepository.incrementLikeCount(routeId);
        }
        return new LikeResult(true, readRouteLikeCount(routeId));
    }

    // 取消点赞
    @Transactional
    public LikeResult unlikeRoute(Long routeId, Long userId) {
        SharedRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        int deleted = likeRepository.deleteByRouteIdAndUserId(routeId, user.getId());
        if (deleted > 0) {
            routeRepository.decrementLikeCount(routeId);
        }
        return new LikeResult(false, readRouteLikeCount(routeId));
    }
    
    // 检查用户是否点赞
    public boolean isLikedByUser(Long routeId, Long userId) {
        SharedRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
                
        return likeRepository.existsByRouteAndUser(route, user);
    }

    // 添加评论
    @Transactional
    public RouteComment addComment(Long routeId, Long userId, String content) {
        SharedRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        RouteComment comment = new RouteComment();
        comment.setRoute(route);
        comment.setUser(user);
        comment.setContent(InputSanitizer.requiredTextBlock(content, 1000, "评论内容"));
        
        RouteComment savedComment = commentRepository.save(comment);

        routeRepository.incrementCommentCount(routeId);
        
        return savedComment;
    }

    // 获取评论列表
    @Transactional(readOnly = true)
    public Page<RouteComment> getComments(Long routeId, Pageable pageable) {
        SharedRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));
        return commentRepository.findByRoute(route, pageable);
    }

    @Transactional
    public void deleteComment(Long routeId, Long commentId, Long userId) {
        RouteComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (comment.getRoute() == null || !comment.getRoute().getId().equals(routeId)) {
            throw new BusinessException("Comment does not belong to this route");
        }

        if (comment.getUser() == null || !comment.getUser().getId().equals(userId)) {
            throw new SecurityException("只能删除自己的评论");
        }

        Long targetRouteId = comment.getRoute().getId();
        commentRepository.delete(comment);
        routeRepository.decrementCommentCount(targetRouteId);
    }

    // 获取用户创建的路线列表
    @Transactional(readOnly = true)
    public Page<SharedRouteSummaryResponse> getRoutesByAuthor(User author, Pageable pageable) {
        if (author == null || author.getId() == null) {
            throw new IllegalArgumentException("Author is required");
        }
        return getRoutesByAuthor(author.getId(), pageable);
    }

    @Transactional(readOnly = true)
    public Page<SharedRouteSummaryResponse> getRoutesByAuthor(Long authorId, Pageable pageable) {
        if (authorId == null) {
            throw new IllegalArgumentException("Author is required");
        }
        return routeRepository.findSummariesByAuthorId(authorId, pageable)
                .map(row -> toSummaryResponse(row, authorId));
    }

    private SharedRouteSummaryResponse toSummaryResponse(SharedRouteSummaryRow row, Long currentUserId) {
        if (row == null) {
            return null;
        }
        return new SharedRouteSummaryResponse(
                row.id(),
                publicAuthor(row.authorId(), row.authorUsername(), row.authorNickname(), row.authorAvatar(), currentUserId),
                row.title(),
                row.days(),
                row.budget(),
                row.preference(),
                row.sourceType(),
                row.sourceRouteId(),
                row.price(),
                row.difficulty(),
                row.temperature(),
                row.geography(),
                row.viewCount(),
                row.likeCount(),
                row.commentCount(),
                row.createdAt(),
                row.updatedAt());
    }

    private PublicUserResponse publicAuthor(
            Long authorId,
            String username,
            String nickname,
            String avatar,
            Long currentUserId) {
        if (authorId == null && !StringUtils.hasText(username) && !StringUtils.hasText(nickname) && !StringUtils.hasText(avatar)) {
            return null;
        }
        return new PublicUserResponse(
                publicNickname(username, nickname),
                avatar,
                currentUserId != null && currentUserId.equals(authorId));
    }

    private String publicNickname(String username, String nickname) {
        if (!StringUtils.hasText(nickname)) {
            return null;
        }
        String normalizedNickname = nickname.trim();
        if (StringUtils.hasText(username) && normalizedNickname.equalsIgnoreCase(username.trim())) {
            return null;
        }
        return normalizedNickname;
    }

    private int readRouteLikeCount(Long routeId) {
        return routeRepository.findLikeCountById(routeId)
                .orElseThrow(() -> new ResourceNotFoundException("Route not found"));
    }

    private Integer validateDays(Integer days) {
        if (days == null || days < 1 || days > 15) {
            throw new IllegalArgumentException("行程天数必须在1到15天之间");
        }
        return days;
    }

    private String normalizeBudget(String budget) {
        String normalized = InputSanitizer.optionalAllowedValue(
                BUDGET_ALIASES.getOrDefault(normalizeAliasKey(budget), budget),
                ALLOWED_BUDGETS,
                "预算");
        return normalized;
    }

    private String normalizePreference(String preference) {
        String normalized = InputSanitizer.optionalAllowedValue(
                PREFERENCE_ALIASES.getOrDefault(normalizeAliasKey(preference), preference),
                ALLOWED_PREFERENCES,
                "旅行偏好");
        return normalized;
    }

    private String normalizeAliasKey(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
