package com.tibet.tourism.modules.content.infra;
import com.tibet.tourism.modules.content.domain.HeritageLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HeritageLikeRepository extends JpaRepository<HeritageLike, Long> {
    boolean existsByUserIdAndHeritageItemId(Long userId, Long heritageItemId);
    long deleteByUserIdAndHeritageItemId(Long userId, Long heritageItemId);
    int countByHeritageItemId(Long heritageItemId);
    void deleteByHeritageItemId(Long heritageItemId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT IGNORE INTO heritage_likes (user_id, heritage_item_id, created_at)
            VALUES (:userId, :heritageItemId, CURRENT_TIMESTAMP)
            """, nativeQuery = true)
    int insertIgnore(@Param("userId") Long userId, @Param("heritageItemId") Long heritageItemId);
}
