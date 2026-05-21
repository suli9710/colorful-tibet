package com.tibet.tourism.modules.content.infra;
import com.tibet.tourism.modules.content.domain.HeritageEvent;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HeritageEventRepository extends JpaRepository<HeritageEvent, Long> {
    List<HeritageEvent> findByHeritageItemId(Long heritageItemId);
    Page<HeritageEvent> findByEventDateGreaterThanEqualOrderByEventDateAsc(LocalDate date, Pageable pageable);
    void deleteByHeritageItemId(Long heritageItemId);
}
