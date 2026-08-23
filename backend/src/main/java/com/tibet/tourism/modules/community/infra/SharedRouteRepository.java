package com.tibet.tourism.modules.community.infra;
import com.tibet.tourism.modules.community.domain.SharedRoute;
import com.tibet.tourism.modules.user.domain.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SharedRouteRepository extends JpaRepository<SharedRoute, Long>, JpaSpecificationExecutor<SharedRoute> {
    
    // 获取用户分享的路线（解决 N+1：一次性加载 author）
    @EntityGraph(attributePaths = {"author"})
    Page<SharedRoute> findByAuthor(User author, Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    List<SharedRoute> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"author"})
    Page<SharedRoute> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Optional<SharedRoute> findBySourceTypeAndSourceRouteId(SharedRoute.SourceType sourceType, Long sourceRouteId);

    long countBySourceType(SharedRoute.SourceType sourceType);

    @Query("SELECT COALESCE(r.likeCount, 0) FROM SharedRoute r WHERE r.id = :id")
    Optional<Integer> findLikeCountById(@Param("id") Long id);

    @Query(value = """
            SELECT new com.tibet.tourism.modules.community.infra.SharedRouteSummaryRow(
                r.id,
                a.id,
                a.username,
                a.nickname,
                a.avatar,
                r.title,
                r.days,
                r.budget,
                r.preference,
                r.sourceType,
                r.sourceRouteId,
                r.price,
                r.difficulty,
                r.temperature,
                r.geography,
                r.viewCount,
                r.likeCount,
                r.commentCount,
                r.createdAt,
                r.updatedAt
            )
            FROM SharedRoute r
            LEFT JOIN r.author a
            WHERE (:days IS NULL OR r.days = :days)
              AND (:budget IS NULL OR r.budget = :budget)
              AND (:preference IS NULL OR r.preference = :preference)
            """,
            countQuery = """
            SELECT COUNT(r)
            FROM SharedRoute r
            WHERE (:days IS NULL OR r.days = :days)
              AND (:budget IS NULL OR r.budget = :budget)
              AND (:preference IS NULL OR r.preference = :preference)
            """)
    Page<SharedRouteSummaryRow> findSummaries(
            @Param("days") Integer days,
            @Param("budget") String budget,
            @Param("preference") String preference,
            Pageable pageable);

    @Query(value = """
            SELECT new com.tibet.tourism.modules.community.infra.SharedRouteSummaryRow(
                r.id,
                a.id,
                a.username,
                a.nickname,
                a.avatar,
                r.title,
                r.days,
                r.budget,
                r.preference,
                r.sourceType,
                r.sourceRouteId,
                r.price,
                r.difficulty,
                r.temperature,
                r.geography,
                r.viewCount,
                r.likeCount,
                r.commentCount,
                r.createdAt,
                r.updatedAt
            )
            FROM SharedRoute r
            JOIN r.author a
            WHERE a.id = :authorId
            """,
            countQuery = """
            SELECT COUNT(r)
            FROM SharedRoute r
            WHERE r.author.id = :authorId
            """)
    Page<SharedRouteSummaryRow> findSummariesByAuthorId(@Param("authorId") Long authorId, Pageable pageable);
    
    // 简单的筛选查询（更复杂的筛选将使用Specification）
    Page<SharedRoute> findByDays(Integer days, Pageable pageable);
    Page<SharedRoute> findByBudget(String budget, Pageable pageable);
    Page<SharedRoute> findByPreference(String preference, Pageable pageable);

    // 根据作者删除路线
    void deleteByAuthor(User author);

    // ── 原子计数更新（绕过乐观锁 read-modify-write 冲突） ──

    @Modifying
    @Query("UPDATE SharedRoute r SET r.viewCount = r.viewCount + 1, r.version = r.version + 1 WHERE r.id = :id")
    int incrementViewCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE SharedRoute r SET r.likeCount = r.likeCount + 1, r.version = r.version + 1 WHERE r.id = :id")
    int incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE SharedRoute r SET r.likeCount = CASE WHEN r.likeCount > 0 THEN r.likeCount - 1 ELSE 0 END, r.version = r.version + 1 WHERE r.id = :id")
    int decrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE SharedRoute r SET r.commentCount = r.commentCount + 1, r.version = r.version + 1 WHERE r.id = :id")
    int incrementCommentCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE SharedRoute r SET r.commentCount = CASE WHEN r.commentCount > 0 THEN r.commentCount - 1 ELSE 0 END, r.version = r.version + 1 WHERE r.id = :id")
    int decrementCommentCount(@Param("id") Long id);

    /** Recomputes like_count from the surviving rows after a bulk delete of one user's likes. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SharedRoute r SET r.likeCount = (SELECT COUNT(rl) FROM RouteLike rl WHERE rl.route.id = r.id), "
            + "r.version = r.version + 1 WHERE r.id IN :ids")
    void recountLikes(@Param("ids") java.util.Collection<Long> ids);

    /** Recomputes comment_count from the surviving rows after a bulk delete of one user's comments. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE SharedRoute r SET r.commentCount = (SELECT COUNT(rc) FROM RouteComment rc WHERE rc.route.id = r.id), "
            + "r.version = r.version + 1 WHERE r.id IN :ids")
    void recountComments(@Param("ids") java.util.Collection<Long> ids);
}
