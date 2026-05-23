package com.tibet.tourism.modules.community.infra;
import com.tibet.tourism.modules.community.domain.Favorite;
import com.tibet.tourism.modules.community.domain.TravelRoute;
import com.tibet.tourism.modules.user.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Page<Favorite> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    List<Favorite> findByUser(User user);
    Optional<Favorite> findByUserAndRoute(User user, TravelRoute route);
    boolean existsByUserAndRoute(User user, TravelRoute route);
    void deleteByUserAndRoute(User user, TravelRoute route);
    void deleteByUser(User user);
    long countByRoute(TravelRoute route);
}
