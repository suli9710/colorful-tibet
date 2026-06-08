package com.tibet.tourism.modules.content.infra;
import com.tibet.tourism.modules.content.domain.HeritageInheritor;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HeritageInheritorRepository extends JpaRepository<HeritageInheritor, Long> {
    List<HeritageInheritor> findByHeritageItemId(Long heritageItemId);
    Page<HeritageInheritor> findByHeritageItemId(Long heritageItemId, Pageable pageable);
    void deleteByHeritageItemId(Long heritageItemId);
}
