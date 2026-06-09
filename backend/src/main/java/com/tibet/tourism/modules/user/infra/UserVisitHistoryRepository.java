package com.tibet.tourism.modules.user.infra;
import com.tibet.tourism.modules.user.domain.UserVisitHistory;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserVisitHistoryRepository extends JpaRepository<UserVisitHistory, Long> {
    int DEFAULT_RECOMMENDATION_HISTORY_LIMIT = 200;

    List<UserVisitHistory> findByUserId(Long userId);
    List<UserVisitHistory> findBySpotId(Long spotId);
    List<UserVisitHistory> findBySpotIdIn(List<Long> spotIds);
    List<UserVisitHistory> findByUserIdIn(List<Long> userIds);
    Optional<UserVisitHistory> findTopByUserIdAndSpotIdOrderByVisitDateDescIdDesc(Long userId, Long spotId);

    default List<UserVisitHistory> findRecentByUserId(Long userId) {
        return findRecentByUserId(userId, DEFAULT_RECOMMENDATION_HISTORY_LIMIT);
    }

    default List<UserVisitHistory> findRecentByUserId(Long userId, int limit) {
        return findRecentByUserId(userId, recentVisitPage(limit));
    }

    @Query("SELECT h FROM UserVisitHistory h WHERE h.user.id = :userId ORDER BY h.visitDate DESC, h.id DESC")
    List<UserVisitHistory> findRecentByUserId(@Param("userId") Long userId, Pageable pageable);

    default List<UserVisitHistory> findRecentBySpotId(Long spotId, int limit) {
        return findRecentBySpotId(spotId, recentVisitPage(limit));
    }

    @Query("SELECT h FROM UserVisitHistory h WHERE h.spot.id = :spotId ORDER BY h.visitDate DESC, h.id DESC")
    List<UserVisitHistory> findRecentBySpotId(@Param("spotId") Long spotId, Pageable pageable);

    default List<UserVisitHistory> findRecentBySpotIdIn(Collection<Long> spotIds) {
        if (spotIds == null || spotIds.isEmpty()) {
            return Collections.emptyList();
        }
        return findRecentBySpotIdIn(spotIds, recentVisitPage(DEFAULT_RECOMMENDATION_HISTORY_LIMIT));
    }

    @Query("SELECT h FROM UserVisitHistory h WHERE h.spot.id IN :spotIds ORDER BY h.visitDate DESC, h.id DESC")
    List<UserVisitHistory> findRecentBySpotIdIn(@Param("spotIds") Collection<Long> spotIds, Pageable pageable);

    default List<UserVisitHistory> findRecentByUserIdIn(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Collections.emptyList();
        }
        return findRecentByUserIdIn(userIds, recentVisitPage(DEFAULT_RECOMMENDATION_HISTORY_LIMIT));
    }

    @Query("SELECT h FROM UserVisitHistory h WHERE h.user.id IN :userIds ORDER BY h.visitDate DESC, h.id DESC")
    List<UserVisitHistory> findRecentByUserIdIn(@Param("userIds") Collection<Long> userIds, Pageable pageable);

    default List<UserVisitHistory> findRecentForRecommendation(int limit) {
        return findAll(recentVisitPage(limit)).getContent();
    }

    static PageRequest recentVisitPage(int limit) {
        int boundedLimit = Math.max(1, limit);
        return PageRequest.of(0, boundedLimit, Sort.by(
                Sort.Order.desc("visitDate"),
                Sort.Order.desc("id")));
    }

    @Query("SELECT h.spot.id, COUNT(h) FROM UserVisitHistory h WHERE h.spot.id IN :spotIds GROUP BY h.spot.id")
    List<Object[]> countBySpotIds(@Param("spotIds") List<Long> spotIds);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE UserVisitHistory h
            SET h.clickCount = COALESCE(h.clickCount, 0) + 1,
                h.visitDate = :visitDate
            WHERE h.user.id = :userId AND h.spot.id = :spotId
            """)
    int incrementSpotView(
            @Param("userId") Long userId,
            @Param("spotId") Long spotId,
            @Param("visitDate") java.time.LocalDateTime visitDate);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO user_visit_history (user_id, spot_id, click_count, visit_date)
            VALUES (:userId, :spotId, 1, :visitDate)
            ON DUPLICATE KEY UPDATE
                click_count = COALESCE(click_count, 0) + 1,
                visit_date = VALUES(visit_date)
            """, nativeQuery = true)
    int upsertSpotView(
            @Param("userId") Long userId,
            @Param("spotId") Long spotId,
            @Param("visitDate") java.time.LocalDateTime visitDate);

    // 根据用户ID删除访问历史
    void deleteByUserId(Long userId);
    void deleteBySpotId(Long spotId);
}
