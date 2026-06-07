package com.tibet.tourism.modules.admin.web;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.admin.web.dto.RouteManagementRequest;
import com.tibet.tourism.modules.community.domain.SharedRoute;
import com.tibet.tourism.modules.community.domain.TravelRoute;
import com.tibet.tourism.modules.community.infra.RouteCommentRepository;
import com.tibet.tourism.modules.community.infra.RouteLikeRepository;
import com.tibet.tourism.modules.community.infra.SharedRouteRepository;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import static com.tibet.tourism.common.validation.RequestParseUtils.isBlank;
import static com.tibet.tourism.modules.admin.web.mapper.AdminDtoMapper.toAdminSharedRoute;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminRouteController {

    private static final Set<String> ALLOWED_ROUTE_BUDGETS = Set.of("经济型", "舒适型", "豪华型");
    private static final Set<String> ALLOWED_ROUTE_PREFERENCES = Set.of("自然风光", "人文历史", "深度摄影", "休闲度假");

    private final SharedRouteRepository sharedRouteRepository;
    private final RouteLikeRepository routeLikeRepository;
    private final RouteCommentRepository routeCommentRepository;
    private final UserRepository userRepository;

    public AdminRouteController(SharedRouteRepository sharedRouteRepository,
                                RouteLikeRepository routeLikeRepository,
                                RouteCommentRepository routeCommentRepository,
                                UserRepository userRepository) {
        this.sharedRouteRepository = sharedRouteRepository;
        this.routeLikeRepository = routeLikeRepository;
        this.routeCommentRepository = routeCommentRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/routes")
    public ResponseEntity<Page<Map<String, Object>>> getAllRoutes(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(sharedRouteRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(route -> toAdminSharedRoute(route)));
    }

    @PostMapping("/routes")
    @Transactional
    public ResponseEntity<?> createRoute(@Valid @RequestBody RouteManagementRequest request, Authentication authentication) {
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
    @Transactional
    public ResponseEntity<?> updateRoute(@PathVariable Long id, @Valid @RequestBody RouteManagementRequest request) {
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
    @Transactional
    public ResponseEntity<?> deleteRoute(@PathVariable Long id) {
        Optional<SharedRoute> routeOpt = sharedRouteRepository.findById(id);
        if (routeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        deleteSharedRouteWithChildren(routeOpt.get());
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    private ResponseEntity<?> applyRouteManagementPayload(SharedRoute route, RouteManagementRequest request, boolean creating) {
        String title = request.effectiveTitle();
        if (creating || request.hasTitleKey()) {
            if (isBlank(title)) {
                return ResponseEntity.badRequest().body(Map.of("error", "线路名称不能为空"));
            }
            route.setTitle(InputSanitizer.requiredPlainText(title, 200, "线路名称"));
        }

        String content = request.effectiveContent();
        if (creating || request.hasContentKey()) {
            route.setContent(isBlank(content)
                    ? route.getTitle()
                    : InputSanitizer.requiredTextBlock(content, 12000, "线路内容"));
        }

        if (creating || request.getDays() != null) {
            Integer days = request.getDays();
            if (days == null || days < 1 || days > 15) {
                return ResponseEntity.badRequest().body(Map.of("error", "天数必须在1到15之间"));
            }
            route.setDays(days);
        }

        if (request.getBudget() != null) {
            route.setBudget(InputSanitizer.optionalAllowedValue(
                    request.getBudget(), ALLOWED_ROUTE_BUDGETS, "预算"));
        } else if (creating && route.getBudget() == null) {
            route.setBudget(resolveBudgetLabel(route.getPrice()));
        }

        if (request.getPreference() != null) {
            route.setPreference(InputSanitizer.optionalAllowedValue(
                    request.getPreference(), ALLOWED_ROUTE_PREFERENCES, "旅行偏好"));
        } else if (creating && route.getPreference() == null) {
            route.setPreference("人文历史");
        }

        if (request.getPrice() != null) {
            BigDecimal price = request.getPrice();
            route.setPrice(price);
            if (request.getBudget() == null || isBlank(route.getBudget())) {
                route.setBudget(resolveBudgetLabel(price));
            }
        }

        if (request.getDifficulty() != null) {
            String difficulty = request.getDifficulty().trim();
            if (difficulty.isEmpty()) {
                route.setDifficulty(null);
            } else {
                try {
                    route.setDifficulty(TravelRoute.Difficulty.valueOf(difficulty.toUpperCase()).name());
                } catch (IllegalArgumentException e) {
                    return ResponseEntity.badRequest().body(Map.of("error", "无效的难度"));
                }
            }
        }

        if (request.getTemperature() != null) {
            route.setTemperature(InputSanitizer.optionalPlainText(request.getTemperature(), 100, "温度说明"));
        }
        if (request.getGeography() != null) {
            route.setGeography(InputSanitizer.optionalPlainText(request.getGeography(), 100, "地理说明"));
        }
        return null;
    }

    private void deleteSharedRouteWithChildren(SharedRoute route) {
        routeLikeRepository.deleteByRoute(route);
        routeCommentRepository.deleteByRoute(route);
        sharedRouteRepository.delete(route);
    }

    private Optional<User> findAuthenticatedUser(Authentication authentication) {
        if (authentication == null || isBlank(authentication.getName())) {
            return Optional.empty();
        }
        return userRepository.findByUsername(authentication.getName());
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
}
