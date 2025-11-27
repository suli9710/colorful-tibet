package com.tibet.tourism.service;

import com.tibet.tourism.entity.*;
import com.tibet.tourism.repository.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class SharedRouteService {

    @Autowired
    private SharedRouteRepository routeRepository;

    @Autowired
    private RouteCommentRepository commentRepository;

    @Autowired
    private RouteLikeRepository likeRepository;

    @Autowired
    private UserRepository userRepository;

    // 分享路线
    @Transactional
    public SharedRoute shareRoute(Long userId, String title, String content, Integer days, String budget, String preference) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        SharedRoute route = new SharedRoute();
        route.setAuthor(user);
        route.setTitle(title);
        route.setContent(content);
        route.setDays(days);
        route.setBudget(budget);
        route.setPreference(preference);

        return routeRepository.save(route);
    }

    // 获取路线列表（带筛选）
    public Page<SharedRoute> getRoutes(Integer days, String budget, String preference, Pageable pageable) {
        return routeRepository.findAll((Specification<SharedRoute>) (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (days != null) {
                predicates.add(criteriaBuilder.equal(root.get("days"), days));
            }
            if (StringUtils.hasText(budget)) {
                predicates.add(criteriaBuilder.equal(root.get("budget"), budget));
            }
            if (StringUtils.hasText(preference)) {
                predicates.add(criteriaBuilder.equal(root.get("preference"), preference));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    // 获取路线详情
    @Transactional
    public SharedRoute getRoute(Long id) {
        SharedRoute route = routeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Route not found"));
        
        // 增加浏览量
        route.incrementViewCount();
        return routeRepository.save(route);
    }

    // 删除路线
    @Transactional
    public void deleteRoute(Long routeId, Long userId) {
        SharedRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("Route not found"));

        if (!route.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You can only delete your own routes");
        }

        routeRepository.delete(route);
    }

    // 点赞路线
    @Transactional
    public boolean likeRoute(Long routeId, Long userId) {
        SharedRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("Route not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (likeRepository.existsByRouteAndUser(route, user)) {
            return false; // 已经点赞过
        }

        RouteLike like = new RouteLike();
        like.setRoute(route);
        like.setUser(user);
        likeRepository.save(like);

        route.incrementLikeCount();
        routeRepository.save(route);
        return true;
    }

    // 取消点赞
    @Transactional
    public boolean unlikeRoute(Long routeId, Long userId) {
        SharedRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("Route not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Optional<RouteLike> like = likeRepository.findByRouteAndUser(route, user);
        if (like.isEmpty()) {
            return false; // 未点赞
        }

        likeRepository.delete(like.get());

        route.decrementLikeCount();
        routeRepository.save(route);
        return true;
    }
    
    // 检查用户是否点赞
    public boolean isLikedByUser(Long routeId, Long userId) {
        SharedRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("Route not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
                
        return likeRepository.existsByRouteAndUser(route, user);
    }

    // 添加评论
    @Transactional
    public RouteComment addComment(Long routeId, Long userId, String content) {
        SharedRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("Route not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        RouteComment comment = new RouteComment();
        comment.setRoute(route);
        comment.setUser(user);
        comment.setContent(content);
        
        RouteComment savedComment = commentRepository.save(comment);

        route.incrementCommentCount();
        routeRepository.save(route);
        
        return savedComment;
    }

    // 获取评论列表
    public List<RouteComment> getComments(Long routeId) {
        SharedRoute route = routeRepository.findById(routeId)
                .orElseThrow(() -> new RuntimeException("Route not found"));
        return commentRepository.findByRouteOrderByCreatedAtDesc(route);
    }
}
