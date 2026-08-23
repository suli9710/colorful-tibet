package com.tibet.tourism.modules.community.infra;
import com.tibet.tourism.modules.community.domain.Comment;
import com.tibet.tourism.modules.user.domain.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    @EntityGraph(attributePaths = {"user", "spot"})
    Page<Comment> findByUser(User user, Pageable pageable);

    long countByUser(User user);
    void deleteByUser(User user);
    void deleteBySpotId(Long spotId);

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = COALESCE(c.likeCount, 0) + 1, c.version = COALESCE(c.version, 0) + 1 WHERE c.id = :id")
    int incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE Comment c SET c.likeCount = CASE WHEN COALESCE(c.likeCount, 0) > 0 THEN c.likeCount - 1 ELSE 0 END, c.version = COALESCE(c.version, 0) + 1 WHERE c.id = :id")
    int decrementLikeCount(@Param("id") Long id);

    /**
     * Recomputes like_count from the surviving rows. Bulk-deleting a user's likes leaves the
     * denormalised counters on other users' comments permanently inflated; recounting also repairs
     * any drift that already accumulated.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE Comment c SET c.likeCount = (SELECT COUNT(cl) FROM CommentLike cl WHERE cl.comment.id = c.id), "
            + "c.version = COALESCE(c.version, 0) + 1 WHERE c.id IN :ids")
    void recountLikes(@Param("ids") java.util.Collection<Long> ids);
}
