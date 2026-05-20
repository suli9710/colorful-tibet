package com.tibet.tourism.modules.community.infra;
import com.tibet.tourism.modules.community.domain.Comment;
import com.tibet.tourism.modules.user.domain.User;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

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
    long countByUser(User user);
    void deleteByUser(User user);
    void deleteBySpotId(Long spotId);
}
