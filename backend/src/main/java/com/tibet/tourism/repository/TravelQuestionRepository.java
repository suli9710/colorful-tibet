package com.tibet.tourism.repository;

import com.tibet.tourism.entity.TravelQuestion;
import com.tibet.tourism.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TravelQuestionRepository extends JpaRepository<TravelQuestion, Long>, JpaSpecificationExecutor<TravelQuestion> {

    @EntityGraph(attributePaths = {"author"})
    List<TravelQuestion> findByAuthorOrderByCreatedAtDesc(User author);

    @EntityGraph(attributePaths = {"author"})
    Page<TravelQuestion> findAll(Pageable pageable);

    void deleteByAuthor(User author);
}
