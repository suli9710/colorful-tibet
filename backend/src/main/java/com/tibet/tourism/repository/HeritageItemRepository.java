package com.tibet.tourism.repository;

import com.tibet.tourism.entity.HeritageItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HeritageItemRepository extends JpaRepository<HeritageItem, Long> {
    Page<HeritageItem> findByCategory(String category, Pageable pageable);
}
