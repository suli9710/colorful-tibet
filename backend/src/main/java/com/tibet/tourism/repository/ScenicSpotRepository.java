package com.tibet.tourism.repository;

import com.tibet.tourism.entity.ScenicSpot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ScenicSpotRepository extends JpaRepository<ScenicSpot, Long> {
    
    @Override
    @Query("SELECT DISTINCT s FROM ScenicSpot s LEFT JOIN FETCH s.tags")
    List<ScenicSpot> findAll();
    
    // 不加载 tags 的查询方法，用于 admin 接口，避免懒加载问题
    @Query("SELECT s FROM ScenicSpot s")
    List<ScenicSpot> findAllWithoutTags();
    
    @Query("SELECT DISTINCT s FROM ScenicSpot s LEFT JOIN FETCH s.tags WHERE s.id = :id")
    Optional<ScenicSpot> findByIdWithTags(@Param("id") Long id);
    
    @Query("SELECT DISTINCT s FROM ScenicSpot s LEFT JOIN FETCH s.tags WHERE s.category = :category")
    List<ScenicSpot> findByCategory(@Param("category") ScenicSpot.Category category);
    
    @Query("SELECT DISTINCT s FROM ScenicSpot s LEFT JOIN FETCH s.tags WHERE s.name LIKE %:keyword%")
    List<ScenicSpot> findByNameContaining(@Param("keyword") String name);
}
