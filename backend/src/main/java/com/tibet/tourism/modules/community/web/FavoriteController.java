package com.tibet.tourism.modules.community.web;
import com.tibet.tourism.modules.community.domain.Favorite;
import com.tibet.tourism.modules.community.domain.TravelRoute;
import com.tibet.tourism.modules.community.infra.FavoriteRepository;
import com.tibet.tourism.modules.community.infra.TravelRouteRepository;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/favorites")
@PreAuthorize("isAuthenticated()")
public class FavoriteController {

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
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }
        Page<Favorite> favorites = favoriteRepository.findByUserOrderByCreatedAtDesc(user, pageable);
        return ResponseEntity.ok(favorites);
    }

    @PostMapping("/{routeId}")
    public ResponseEntity<?> addFavorite(@PathVariable Long routeId) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }
        TravelRoute route = travelRouteRepository.findById(routeId).orElse(null);
        if (route == null) {
            return ResponseEntity.notFound().build();
        }
        if (favoriteRepository.existsByUserAndRoute(user, route)) {
            return ResponseEntity.ok(Map.of("message", "已收藏", "favorited", true));
        }
        Favorite favorite = new Favorite();
        favorite.setUser(user);
        favorite.setRoute(route);
        favoriteRepository.save(favorite);
        long count = favoriteRepository.countByRoute(route);
        return ResponseEntity.ok(Map.of("message", "收藏成功", "favorited", true, "favoriteCount", count));
    }

    @DeleteMapping("/{routeId}")
    @Transactional
    public ResponseEntity<?> removeFavorite(@PathVariable Long routeId) {
        User user = getCurrentUser();
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "未登录"));
        }
        TravelRoute route = travelRouteRepository.findById(routeId).orElse(null);
        if (route == null) {
            return ResponseEntity.notFound().build();
        }
        favoriteRepository.deleteByUserAndRoute(user, route);
        long count = favoriteRepository.countByRoute(route);
        return ResponseEntity.ok(Map.of("message", "取消收藏", "favorited", false, "favoriteCount", count));
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
}
