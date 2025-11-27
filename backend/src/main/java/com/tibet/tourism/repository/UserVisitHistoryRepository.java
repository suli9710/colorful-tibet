package com.tibet.tourism.repository;

import com.tibet.tourism.entity.UserVisitHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface UserVisitHistoryRepository extends JpaRepository<UserVisitHistory, Long> {
    List<UserVisitHistory> findByUserId(Long userId);
    List<UserVisitHistory> findBySpotId(Long spotId);
    List<UserVisitHistory> findBySpotIdIn(List<Long> spotIds);
    List<UserVisitHistory> findByUserIdIn(List<Long> userIds);
}
