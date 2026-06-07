package com.tibet.tourism.modules.content.infra;
import com.tibet.tourism.modules.content.domain.TibetanDictionary;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface TibetanDictionaryRepository extends JpaRepository<TibetanDictionary, Long> {
    
    /**
     * 根据中文文本和类型查找
     */
    Optional<TibetanDictionary> findByChineseTextAndType(String chineseText, TibetanDictionary.Type type);
    
    /**
     * 根据中文文本查找（所有类型）
     */
    List<TibetanDictionary> findByChineseText(String chineseText);
    
    /**
     * 根据类型查找
     */
    List<TibetanDictionary> findByType(TibetanDictionary.Type type);

    List<TibetanDictionary> findByType(TibetanDictionary.Type type, Pageable pageable);
    
    /**
     * 模糊搜索中文文本
     */
    @Query("SELECT d FROM TibetanDictionary d WHERE d.chineseText LIKE %:keyword% ORDER BY d.usageCount DESC")
    List<TibetanDictionary> searchByChineseText(@Param("keyword") String keyword);

    @Query("SELECT d FROM TibetanDictionary d WHERE d.chineseText LIKE %:keyword% ORDER BY d.usageCount DESC")
    List<TibetanDictionary> searchByChineseText(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 模糊搜索藏语文本
     */
    @Query("SELECT d FROM TibetanDictionary d WHERE d.tibetanText LIKE %:keyword% ORDER BY d.usageCount DESC")
    List<TibetanDictionary> searchByTibetanText(@Param("keyword") String keyword);

    @Query("SELECT d FROM TibetanDictionary d WHERE d.tibetanText LIKE %:keyword% ORDER BY d.usageCount DESC")
    List<TibetanDictionary> searchByTibetanText(@Param("keyword") String keyword, Pageable pageable);
    
    /**
     * 获取使用频率最高的词条
     */
    @Query("SELECT d FROM TibetanDictionary d ORDER BY d.usageCount DESC")
    List<TibetanDictionary> findTopByUsageCount(Pageable pageable);
}

















