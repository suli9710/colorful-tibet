package com.tibet.tourism.modules.community.infra;
import com.tibet.tourism.modules.community.domain.CommentLike;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {
    Optional<CommentLike> findByUserIdAndCommentId(Long userId, Long commentId);
    boolean existsByUserIdAndCommentId(Long userId, Long commentId);
    int countByCommentId(Long commentId);
    void deleteByUserIdAndCommentId(Long userId, Long commentId);
    void deleteByCommentId(Long commentId);

    // 根据用户ID删除所有点赞
    void deleteByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.user.id = :userId")
    void deleteByCommentUserId(@Param("userId") Long userId);
}


