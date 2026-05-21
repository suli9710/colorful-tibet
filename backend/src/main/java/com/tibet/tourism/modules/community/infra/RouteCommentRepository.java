package com.tibet.tourism.modules.community.infra;
import com.tibet.tourism.modules.community.domain.RouteComment;
import com.tibet.tourism.modules.community.domain.SharedRoute;
import com.tibet.tourism.modules.user.domain.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RouteCommentRepository extends JpaRepository<RouteComment, Long> {
    
    // 获取路线的所有评论，按时间倒序（解决 N+1：一次性加载 user）
    @EntityGraph(attributePaths = {"user"})
    List<RouteComment> findByRouteOrderByCreatedAtDesc(SharedRoute route);

    @EntityGraph(attributePaths = {"route", "user"})
    List<RouteComment> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"route", "user"})
    Page<RouteComment> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    // 统计路线评论数
    long countByRoute(SharedRoute route);
    
    // 获取用户的所有评论，按时间倒序
    @EntityGraph(attributePaths = {"route"})
    List<RouteComment> findByUserOrderByCreatedAtDesc(User user);
    
    // 统计用户评论数
    long countByUser(User user);

    // 根据用户删除路线评论
    void deleteByUser(User user);

    void deleteByRoute(SharedRoute route);
}
