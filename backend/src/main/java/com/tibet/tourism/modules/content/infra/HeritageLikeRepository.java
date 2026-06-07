package com.tibet.tourism.modules.content.infra;
import com.tibet.tourism.modules.content.domain.HeritageLike;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HeritageLikeRepository extends JpaRepository<HeritageLike, Long> {
    boolean existsByUserIdAndHeritageItemId(Long userId, Long heritageItemId);
    long deleteByUserIdAndHeritageItemId(Long userId, Long heritageItemId);
    int countByHeritageItemId(Long heritageItemId);
    void deleteByHeritageItemId(Long heritageItemId);
}
