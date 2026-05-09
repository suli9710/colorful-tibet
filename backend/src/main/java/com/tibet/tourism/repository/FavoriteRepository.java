package com.tibet.tourism.repository;

import com.tibet.tourism.entity.Favorite;
import com.tibet.tourism.entity.TravelRoute;
import com.tibet.tourism.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Page<Favorite> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);
    List<Favorite> findByUser(User user);
    Optional<Favorite> findByUserAndRoute(User user, TravelRoute route);
    boolean existsByUserAndRoute(User user, TravelRoute route);
    void deleteByUserAndRoute(User user, TravelRoute route);
    long countByRoute(TravelRoute route);
}
