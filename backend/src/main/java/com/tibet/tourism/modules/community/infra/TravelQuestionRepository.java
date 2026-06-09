package com.tibet.tourism.modules.community.infra;
import com.tibet.tourism.modules.community.domain.TravelQuestion;
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
public interface TravelQuestionRepository extends JpaRepository<TravelQuestion, Long>, JpaSpecificationExecutor<TravelQuestion> {

    @EntityGraph(attributePaths = {"author"})
    List<TravelQuestion> findByAuthorOrderByCreatedAtDesc(User author);

    @EntityGraph(attributePaths = {"author"})
    Page<TravelQuestion> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"author"})
    List<TravelQuestion> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"author"})
    Page<TravelQuestion> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query("""
            SELECT new com.tibet.tourism.modules.community.infra.TravelQuestionSummaryRow(
                q.id,
                a.id,
                a.username,
                a.nickname,
                a.avatar,
                q.title,
                CASE
                    WHEN q.content IS NULL THEN NULL
                    WHEN LENGTH(q.content) <= 160 THEN q.content
                    ELSE CONCAT(SUBSTRING(q.content, 1, 157), '...')
                END,
                q.tags,
                q.viewCount,
                q.answerCount,
                q.likeCount,
                q.isResolved,
                q.createdAt,
                q.updatedAt
            )
            FROM TravelQuestion q
            JOIN q.author a
            WHERE (:tag IS NULL OR q.tags LIKE CONCAT('%', :tag, '%'))
              AND (:resolved IS NULL OR q.isResolved = :resolved)
            """)
    Page<TravelQuestionSummaryRow> findSummaries(
            @Param("tag") String tag,
            @Param("resolved") Boolean resolved,
            Pageable pageable);

    void deleteByAuthor(User author);

    @Query("SELECT COALESCE(q.likeCount, 0) FROM TravelQuestion q WHERE q.id = :id")
    Optional<Integer> findLikeCountById(@Param("id") Long id);

    // ── 原子计数更新（绕过乐观锁 read-modify-write 冲突） ──

    @Modifying
    @Query("UPDATE TravelQuestion q SET q.viewCount = q.viewCount + 1, q.version = q.version + 1 WHERE q.id = :id")
    int incrementViewCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE TravelQuestion q SET q.likeCount = q.likeCount + 1, q.version = q.version + 1 WHERE q.id = :id")
    int incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE TravelQuestion q SET q.likeCount = CASE WHEN q.likeCount > 0 THEN q.likeCount - 1 ELSE 0 END, q.version = q.version + 1 WHERE q.id = :id")
    int decrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE TravelQuestion q SET q.answerCount = q.answerCount + 1, q.version = q.version + 1 WHERE q.id = :id")
    int incrementAnswerCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE TravelQuestion q SET q.answerCount = CASE WHEN q.answerCount > 0 THEN q.answerCount - 1 ELSE 0 END, q.version = q.version + 1 WHERE q.id = :id")
    int decrementAnswerCount(@Param("id") Long id);
}
