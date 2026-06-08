package com.tibet.tourism.modules.community.web;

import com.tibet.tourism.common.api.PageResponse;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.community.domain.Favorite;
import com.tibet.tourism.modules.community.domain.TravelRoute;
import com.tibet.tourism.modules.community.infra.FavoriteRepository;
import com.tibet.tourism.modules.community.infra.TravelRouteRepository;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/favorites")
@PreAuthorize("isAuthenticated()")
public class FavoriteController {

    private static final Set<String> ALLOWED_FAVORITE_SORT_FIELDS = Set.of("id", "createdAt");
    private static final Sort DEFAULT_FAVORITE_SORT = Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by("id"));

    @Autowired
    private FavoriteRepository favoriteRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TravelRouteRepository travelRouteRepository;

    @GetMapping
    public ResponseEntity<?> getMyFavorites(@PageableDefault(size = 20) Pageable pageable) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }
        Pageable safePageable = InputSanitizer.sanitizePageable(
                pageable, ALLOWED_FAVORITE_SORT_FIELDS, DEFAULT_FAVORITE_SORT, 20, 100);
        Page<FavoriteResponse> favorites = favoriteRepository.findByUserOrderByCreatedAtDesc(user, safePageable)
                .map(FavoriteResponse::from);
        return ResponseEntity.ok(PageResponse.from(favorites));
    }

    @PostMapping("/{routeId}")
    public ResponseEntity<?> addFavorite(@PathVariable Long routeId) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }
        TravelRoute route = travelRouteRepository.findById(routeId).orElse(null);
        if (route == null) {
            return ResponseEntity.notFound().build();
        }
        if (favoriteRepository.existsByUserAndRoute(user, route)) {
            return ResponseEntity.ok(Map.of("message", "Already favorited", "favorited", true));
        }
        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setRoute(route);
        try {
            favoriteRepository.saveAndFlush(favorite);
        } catch (DataIntegrityViolationException duplicate) {
            // Concurrent duplicate favorite; the unique row already represents the desired state.
        }
        long count = favoriteRepository.countByRoute(route);
        return ResponseEntity.ok(Map.of("message", "Favorite added", "favorited", true, "favoriteCount", count));
    }

    @DeleteMapping("/{routeId}")
    @Transactional
    public ResponseEntity<?> removeFavorite(@PathVariable Long routeId) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Authentication required"));
        }
        TravelRoute route = travelRouteRepository.findById(routeId).orElse(null);
        if (route == null) {
            return ResponseEntity.notFound().build();
        }
        favoriteRepository.deleteByUserAndRoute(user, route);
        long count = favoriteRepository.countByRoute(route);
        return ResponseEntity.ok(Map.of("message", "Favorite removed", "favorited", false, "favoriteCount", count));
    }

    @GetMapping("/{routeId}/status")
    public ResponseEntity<?> checkFavorite(@PathVariable Long routeId) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.ok(Map.of("favorited", false));
        }
        TravelRoute route = travelRouteRepository.findById(routeId).orElse(null);
        if (route == null) {
            return ResponseEntity.notFound().build();
        }
        boolean favorited = favoriteRepository.existsByUserAndRoute(user, route);
        long count = favoriteRepository.countByRoute(route);
        return ResponseEntity.ok(Map.of("favorited", favorited, "favoriteCount", count));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetails) {
            return userRepository.findByUsername(((UserDetails) auth.getPrincipal()).getUsername()).orElse(null);
        }
        return null;
    }

    public record FavoriteResponse(
            Long id,
            LocalDateTime createdAt,
            RouteSummary route) {

        private static FavoriteResponse from(Favorite favorite) {
            return new FavoriteResponse(
                    favorite.getId(),
                    favorite.getCreatedAt(),
                    RouteSummary.from(favorite.getRoute()));
        }
    }

    public record RouteSummary(
            Long id,
            String name,
            String nameTibetan,
            String description,
            String descriptionTibetan,
            Integer days,
            BigDecimal price,
            TravelRoute.Difficulty difficulty,
            String temperature,
            String geography) {

        private static RouteSummary from(TravelRoute route) {
            if (route == null) {
                return null;
            }
            return new RouteSummary(
                    route.getId(),
                    route.getName(),
                    route.getNameTibetan(),
                    route.getDescription(),
                    route.getDescriptionTibetan(),
                    route.getDays(),
                    route.getPrice(),
                    route.getDifficulty(),
                    route.getTemperature(),
                    route.getGeography());
        }
    }
}
