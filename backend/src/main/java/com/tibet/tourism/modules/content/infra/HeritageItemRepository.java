package com.tibet.tourism.modules.content.infra;
import com.tibet.tourism.modules.content.domain.HeritageItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Modifying
    @Query("UPDATE HeritageItem h SET h.viewCount = COALESCE(h.viewCount, 0) + 1, h.version = COALESCE(h.version, 0) + 1 WHERE h.id = :id")
    int incrementViewCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE HeritageItem h SET h.likeCount = COALESCE(h.likeCount, 0) + 1, h.version = COALESCE(h.version, 0) + 1 WHERE h.id = :id")
    int incrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE HeritageItem h SET h.likeCount = CASE WHEN COALESCE(h.likeCount, 0) > 0 THEN h.likeCount - 1 ELSE 0 END, h.version = COALESCE(h.version, 0) + 1 WHERE h.id = :id")
    int decrementLikeCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE HeritageItem h SET h.commentCount = COALESCE(h.commentCount, 0) + 1, h.version = COALESCE(h.version, 0) + 1 WHERE h.id = :id")
    int incrementCommentCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE HeritageItem h SET h.commentCount = CASE WHEN COALESCE(h.commentCount, 0) > 0 THEN h.commentCount - 1 ELSE 0 END, h.version = COALESCE(h.version, 0) + 1 WHERE h.id = :id")
    int decrementCommentCount(@Param("id") Long id);
}
