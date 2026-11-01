package com.tibet.tourism.modules.community.infra;
import com.tibet.tourism.modules.community.domain.CommentLike;
import java.util.List;
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
    long deleteByUserIdAndCommentId(Long userId, Long commentId);
    void deleteByCommentId(Long commentId);

    @Query("""
            SELECT cl.comment.id
            FROM CommentLike cl
            WHERE cl.user.id = :userId
              AND cl.comment.id IN :commentIds
            """)
    List<Long> findLikedCommentIds(@Param("userId") Long userId, @Param("commentIds") List<Long> commentIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT IGNORE INTO comment_likes (user_id, comment_id, created_at)
            VALUES (:userId, :commentId, CURRENT_TIMESTAMP)
            """, nativeQuery = true)
    int insertIgnore(@Param("userId") Long userId, @Param("commentId") Long commentId);

    // 根据用户ID删除所有点赞
    void deleteByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM CommentLike cl WHERE cl.comment.user.id = :userId")
    void deleteByCommentUserId(@Param("userId") Long userId);

    /** Comments (owned by other users) whose like_count must be recomputed after this user's likes are deleted. */
    @Query("SELECT DISTINCT cl.comment.id FROM CommentLike cl WHERE cl.user.id = :userId")
    List<Long> findLikedCommentIdsByUserId(@Param("userId") Long userId);
}


