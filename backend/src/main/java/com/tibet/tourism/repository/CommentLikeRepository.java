package com.tibet.tourism.repository;

import com.tibet.tourism.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    Optional<CommentLike> findByUserIdAndCommentId(Long userId, Long commentId);
    boolean existsByUserIdAndCommentId(Long userId, Long commentId);
    int countByCommentId(Long commentId);
    void deleteByUserIdAndCommentId(Long userId, Long commentId);
}



