package com.tibet.tourism.modules.community.infra;
import com.tibet.tourism.modules.community.domain.TravelQuestion;
import com.tibet.tourism.modules.user.domain.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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

    void deleteByAuthor(User author);
}
