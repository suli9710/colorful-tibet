package com.tibet.tourism.modules.content.infra;
import com.tibet.tourism.modules.content.domain.News;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NewsRepository extends JpaRepository<News, Long> {
    List<News> findByCreatedAtAfterOrderByCreatedAtAsc(LocalDateTime createdAt);

    @Query("""
            SELECT n
            FROM News n
            WHERE (:category IS NULL OR n.category = :category)
              AND (
                :keyword IS NULL
                OR LOWER(n.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(n.content) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR (n.titleTibetan IS NOT NULL AND LOWER(n.titleTibetan) LIKE LOWER(CONCAT('%', :keyword, '%')))
                OR (n.contentTibetan IS NOT NULL AND LOWER(n.contentTibetan) LIKE LOWER(CONCAT('%', :keyword, '%')))
              )
            """)
    Page<News> search(
            @Param("category") News.Category category,
            @Param("keyword") String keyword,
            Pageable pageable);
}
