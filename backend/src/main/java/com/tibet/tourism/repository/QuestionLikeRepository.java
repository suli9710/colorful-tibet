package com.tibet.tourism.repository;

import com.tibet.tourism.entity.QuestionLike;
import com.tibet.tourism.entity.TravelQuestion;
import com.tibet.tourism.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QuestionLikeRepository extends JpaRepository<QuestionLike, Long> {

    boolean existsByQuestionAndUser(TravelQuestion question, User user);

    Optional<QuestionLike> findByQuestionAndUser(TravelQuestion question, User user);

    void deleteByQuestion(TravelQuestion question);
}
