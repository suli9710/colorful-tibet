package com.tibet.tourism.modules.content.infra;
import com.tibet.tourism.modules.content.domain.HeritageInheritor;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HeritageInheritorRepository extends JpaRepository<HeritageInheritor, Long> {
    List<HeritageInheritor> findByHeritageItemId(Long heritageItemId);
    void deleteByHeritageItemId(Long heritageItemId);
}
