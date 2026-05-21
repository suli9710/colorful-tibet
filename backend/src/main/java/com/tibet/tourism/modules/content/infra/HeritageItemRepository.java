package com.tibet.tourism.modules.content.infra;
import com.tibet.tourism.modules.content.domain.HeritageItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HeritageItemRepository extends JpaRepository<HeritageItem, Long> {
    Page<HeritageItem> findByCategory(String category, Pageable pageable);
    Page<HeritageItem> findByNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
            String name, String description, Pageable pageable);

    @Query("""
            SELECT h FROM HeritageItem h
            WHERE h.category = :category
              AND (
                LOWER(h.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(COALESCE(h.description, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    Page<HeritageItem> searchByCategoryAndKeyword(
            @Param("category") String category,
            @Param("keyword") String keyword,
            Pageable pageable);
}
