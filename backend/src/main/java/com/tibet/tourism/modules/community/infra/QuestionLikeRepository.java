package com.tibet.tourism.modules.community.infra;
import com.tibet.tourism.modules.community.domain.QuestionLike;
import com.tibet.tourism.modules.community.domain.TravelQuestion;
import com.tibet.tourism.modules.user.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionLikeRepository extends JpaRepository<QuestionLike, Long> {

    boolean existsByQuestionAndUser(TravelQuestion question, User user);

    Optional<QuestionLike> findByQuestionAndUser(TravelQuestion question, User user);

    void deleteByQuestion(TravelQuestion question);

    void deleteByUser(User user);

    @Modifying
    @Query("DELETE FROM QuestionLike ql WHERE ql.question.author.id = :userId")
    void deleteByQuestionAuthorId(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT IGNORE INTO question_likes (question_id, user_id, created_at)
            VALUES (:questionId, :userId, CURRENT_TIMESTAMP)
            """, nativeQuery = true)
    int insertIgnore(@Param("questionId") Long questionId, @Param("userId") Long userId);
}
