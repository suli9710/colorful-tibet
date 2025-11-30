package com.tibet.tourism.repository;

import com.tibet.tourism.entity.RouteComment;
import com.tibet.tourism.entity.SharedRoute;
import com.tibet.tourism.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RouteCommentRepository extends JpaRepository<RouteComment, Long> {
    
    // 获取路线的所有评论，按时间倒序
    List<RouteComment> findByRouteOrderByCreatedAtDesc(SharedRoute route);
    
    // 统计路线评论数
    long countByRoute(SharedRoute route);
    
    // 获取用户的所有评论，按时间倒序
    List<RouteComment> findByUserOrderByCreatedAtDesc(User user);
    
    // 统计用户评论数
    long countByUser(User user);
}
