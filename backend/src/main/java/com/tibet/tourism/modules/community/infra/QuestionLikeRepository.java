package com.tibet.tourism.modules.community.infra;
import com.tibet.tourism.modules.community.domain.QuestionLike;
import com.tibet.tourism.modules.community.domain.TravelQuestion;
import com.tibet.tourism.modules.user.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface QuestionLikeRepository extends JpaRepository<QuestionLike, Long> {

    boolean existsByQuestionAndUser(TravelQuestion question, User user);

    Optional<QuestionLike> findByQuestionAndUser(TravelQuestion question, User user);

    void deleteByQuestion(TravelQuestion question);
}
