package com.tibet.tourism.modules.content.infra;
import com.tibet.tourism.modules.content.domain.News;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NewsRepository extends JpaRepository<News, Long> {
    List<News> findByCreatedAtAfterOrderByCreatedAtAsc(LocalDateTime createdAt);
}
