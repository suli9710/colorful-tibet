package com.tibet.tourism.repository;

import com.tibet.tourism.entity.Comment;
import com.tibet.tourism.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findBySpotIdOrderByCreatedAtDesc(Long spotId);
    Page<Comment> findBySpotIdOrderByCreatedAtDesc(Long spotId, Pageable pageable);
    List<Comment> findByUserOrderByCreatedAtDesc(User user);
    long countByUser(User user);
    void deleteByUser(User user);
}
