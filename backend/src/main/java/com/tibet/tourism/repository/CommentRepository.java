package com.tibet.tourism.repository;

import com.tibet.tourism.entity.Comment;
import com.tibet.tourism.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findBySpotIdOrderByCreatedAtDesc(Long spotId);
    List<Comment> findByUserOrderByCreatedAtDesc(User user);
    long countByUser(User user);
}
