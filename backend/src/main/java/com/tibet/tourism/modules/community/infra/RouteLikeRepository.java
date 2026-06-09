package com.tibet.tourism.modules.community.infra;
import com.tibet.tourism.modules.community.domain.RouteLike;
import com.tibet.tourism.modules.community.domain.SharedRoute;
import com.tibet.tourism.modules.user.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RouteLikeRepository extends JpaRepository<RouteLike, Long> {
    
    // 检查用户是否点赞了某路线
    boolean existsByRouteAndUser(SharedRoute route, User user);
    
    // 获取具体的点赞记录（用于取消点赞）
    @EntityGraph(attributePaths = {"route", "user"})
    Optional<RouteLike> findByRouteAndUser(SharedRoute route, User user);
    
    // 统计路线点赞数
    long countByRoute(SharedRoute route);

    // 根据用户删除所有路线点赞
    void deleteByUser(User user);

    void deleteByRoute(SharedRoute route);

    @Modifying
    @Query("DELETE FROM RouteLike rl WHERE rl.route.id = :routeId")
    void deleteByRouteId(@Param("routeId") Long routeId);

    @Modifying
    @Query("DELETE FROM RouteLike rl WHERE rl.route.author.id = :userId")
    void deleteByRouteAuthorId(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT IGNORE INTO route_likes (route_id, user_id, created_at)
            VALUES (:routeId, :userId, CURRENT_TIMESTAMP)
            """, nativeQuery = true)
    int insertIgnore(@Param("routeId") Long routeId, @Param("userId") Long userId);
}
