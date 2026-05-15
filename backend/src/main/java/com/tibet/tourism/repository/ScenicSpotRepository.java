package com.tibet.tourism.repository;

import com.tibet.tourism.entity.ScenicSpot;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface ScenicSpotRepository extends JpaRepository<ScenicSpot, Long> {

    @Override
    @Query("SELECT DISTINCT s FROM ScenicSpot s LEFT JOIN FETCH s.tags")
    List<ScenicSpot> findAll();

    @Query("SELECT s FROM ScenicSpot s")
    List<ScenicSpot> findAllWithoutTags();

    @Query("SELECT s FROM ScenicSpot s")
    Page<ScenicSpot> findAllWithoutTags(Pageable pageable);

    List<ScenicSpot> findTop8ByOrderByVisitCountDescIdAsc();

    @Query("SELECT s.category, COUNT(s) FROM ScenicSpot s GROUP BY s.category")
    List<Object[]> countByCategoryGroup();

    @Query("SELECT s FROM ScenicSpot s WHERE s.category = :category")
    Page<ScenicSpot> findByCategory(@Param("category") ScenicSpot.Category category, Pageable pageable);

    @Query("SELECT DISTINCT s FROM ScenicSpot s LEFT JOIN FETCH s.tags WHERE s.category = :category")
    List<ScenicSpot> findByCategory(@Param("category") ScenicSpot.Category category);

    @Query("SELECT s FROM ScenicSpot s WHERE s.name LIKE %:keyword%")
    Page<ScenicSpot> findByNameContaining(@Param("keyword") String name, Pageable pageable);

    @Query("SELECT DISTINCT s FROM ScenicSpot s LEFT JOIN FETCH s.tags WHERE s.name LIKE %:keyword%")
    List<ScenicSpot> findByNameContaining(@Param("keyword") String name);

    @Query("SELECT DISTINCT s FROM ScenicSpot s LEFT JOIN FETCH s.tags WHERE s.id = :id")
    Optional<ScenicSpot> findByIdWithTags(@Param("id") Long id);

    @Query("SELECT DISTINCT s FROM ScenicSpot s LEFT JOIN FETCH s.tags st WHERE st.tag IN :tags AND s.id NOT IN :excludeIds")
    List<ScenicSpot> findByTagsInAndIdNotIn(@Param("tags") List<String> tags, @Param("excludeIds") Set<Long> excludeIds);

    @Query("""
        SELECT s
        FROM ScenicSpot s
        WHERE s.longitude IS NOT NULL
          AND s.latitude IS NOT NULL
        ORDER BY s.visitCount DESC, s.id ASC
        """)
    List<ScenicSpot> findHeatmapSpots(Pageable pageable);
}
