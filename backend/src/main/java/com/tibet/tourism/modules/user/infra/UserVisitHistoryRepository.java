package com.tibet.tourism.modules.user.infra;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.domain.UserVisitHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserVisitHistoryRepository extends JpaRepository<UserVisitHistory, Long> {
    List<UserVisitHistory> findByUserId(Long userId);
    List<UserVisitHistory> findBySpotId(Long spotId);
    List<UserVisitHistory> findBySpotIdIn(List<Long> spotIds);
    List<UserVisitHistory> findByUserIdIn(List<Long> userIds);

    @Query("SELECT h.spot.id, COUNT(h) FROM UserVisitHistory h WHERE h.spot.id IN :spotIds GROUP BY h.spot.id")
    List<Object[]> countBySpotIds(@Param("spotIds") List<Long> spotIds);

    // 根据用户ID删除访问历史
    void deleteByUserId(Long userId);
    void deleteBySpotId(Long spotId);
}
