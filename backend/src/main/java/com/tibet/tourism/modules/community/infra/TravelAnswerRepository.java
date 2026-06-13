package com.tibet.tourism.modules.community.infra;
import com.tibet.tourism.modules.community.domain.TravelAnswer;
import com.tibet.tourism.modules.community.domain.TravelQuestion;
import com.tibet.tourism.modules.user.domain.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TravelAnswerRepository extends JpaRepository<TravelAnswer, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<TravelAnswer> findByQuestionOrderByIsAcceptedDescLikeCountDescCreatedAtAsc(TravelQuestion question);

    @EntityGraph(attributePaths = {"user"})
    Page<TravelAnswer> findByQuestion(TravelQuestion question, Pageable pageable);

    @EntityGraph(attributePaths = {"question", "user"})
    List<TravelAnswer> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"question", "user"})
    Page<TravelAnswer> findAllByOrderByCreatedAtDesc(Pageable pageable);

    void deleteByQuestion(TravelQuestion question);

    void deleteByUser(User user);

    @Modifying
    @Query("DELETE FROM TravelAnswer a WHERE a.question.author.id = :userId")
    void deleteByQuestionAuthorId(@Param("userId") Long userId);

    @Modifying
    @Query("UPDATE TravelAnswer a SET a.isAccepted = false "
            + "WHERE a.question.id = :questionId AND a.id <> :answerId AND a.isAccepted = true")
    int clearAcceptedExcept(@Param("questionId") Long questionId, @Param("answerId") Long answerId);
}
