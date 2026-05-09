package com.tibet.tourism.repository;

import com.tibet.tourism.entity.TravelAnswer;
import com.tibet.tourism.entity.TravelQuestion;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravelAnswerRepository extends JpaRepository<TravelAnswer, Long> {

    @EntityGraph(attributePaths = {"user"})
    List<TravelAnswer> findByQuestionOrderByIsAcceptedDescLikeCountDescCreatedAtAsc(TravelQuestion question);

    void deleteByQuestion(TravelQuestion question);
}
