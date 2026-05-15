package com.tibet.tourism.repository;

import com.tibet.tourism.entity.Comment;
import com.tibet.tourism.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = {"user", "spot"})
    List<Comment> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"user", "spot"})
    Page<Comment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    List<Comment> findBySpotIdOrderByCreatedAtDesc(Long spotId);

    @EntityGraph(attributePaths = {"user"})
    Page<Comment> findBySpotIdOrderByCreatedAtDesc(Long spotId, Pageable pageable);

    @EntityGraph(attributePaths = {"spot"})
    List<Comment> findByUserOrderByCreatedAtDesc(User user);
    long countByUser(User user);
    void deleteByUser(User user);
}
