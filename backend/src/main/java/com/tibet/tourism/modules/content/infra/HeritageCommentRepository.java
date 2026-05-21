package com.tibet.tourism.modules.content.infra;
import com.tibet.tourism.modules.content.domain.HeritageComment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HeritageCommentRepository extends JpaRepository<HeritageComment, Long> {
    Page<HeritageComment> findByHeritageItemIdOrderByCreatedAtDesc(Long heritageItemId, Pageable pageable);
    long countByHeritageItemId(Long heritageItemId);
    void deleteByHeritageItemId(Long heritageItemId);
}
