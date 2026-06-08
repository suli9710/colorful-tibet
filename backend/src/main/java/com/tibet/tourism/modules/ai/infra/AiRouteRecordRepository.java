package com.tibet.tourism.modules.ai.infra;

import com.tibet.tourism.modules.ai.domain.AiRouteRecord;
import com.tibet.tourism.modules.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiRouteRecordRepository extends JpaRepository<AiRouteRecord, Long> {

    @EntityGraph(attributePaths = {"user"})
    Optional<AiRouteRecord> findFirstByUserOrderByUpdatedAtDesc(User user);

    @EntityGraph(attributePaths = {"user"})
    List<AiRouteRecord> findByUserAndManuallySavedTrueOrderByUpdatedAtDesc(User user);

    @EntityGraph(attributePaths = {"user"})
    Optional<AiRouteRecord> findByIdAndUser(Long id, User user);

    Optional<AiRouteRecord> findFirstByUserIdAndJobIdOrderByUpdatedAtDesc(Long userId, String jobId);

    List<AiRouteRecord> findByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(
            AiRouteRecord.Status status, LocalDateTime updatedBefore);

    void deleteByUserId(Long userId);
}
