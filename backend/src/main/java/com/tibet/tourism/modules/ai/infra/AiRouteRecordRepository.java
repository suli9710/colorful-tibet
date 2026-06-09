package com.tibet.tourism.modules.ai.infra;

import com.tibet.tourism.modules.ai.domain.AiRouteRecord;
import com.tibet.tourism.modules.user.domain.User;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AiRouteRecordRepository extends JpaRepository<AiRouteRecord, Long> {

    @EntityGraph(attributePaths = {"user"})
    Optional<AiRouteRecord> findFirstByUserOrderByUpdatedAtDesc(User user);

    Page<AiRouteRecordSummaryProjection> findByUserAndManuallySavedTrue(User user, Pageable pageable);

    @EntityGraph(attributePaths = {"user"})
    Optional<AiRouteRecord> findByIdAndUser(Long id, User user);

    @EntityGraph(attributePaths = {"user"})
    Optional<AiRouteRecord> findByIdAndUserAndManuallySavedTrue(Long id, User user);

    Optional<AiRouteRecord> findFirstByUserIdAndJobIdOrderByUpdatedAtDesc(Long userId, String jobId);

    @Query("""
            SELECT r FROM AiRouteRecord r
            WHERE r.status = :status
              AND r.updatedAt < :updatedBefore
            ORDER BY r.updatedAt ASC, r.id ASC
            """)
    List<AiRouteRecord> findStaleRunningRecordsForRecovery(@Param("status") AiRouteRecord.Status status,
                                                            @Param("updatedBefore") LocalDateTime updatedBefore,
                                                            Pageable pageable);

    void deleteByUserId(Long userId);

    interface AiRouteRecordSummaryProjection {
        Long getId();

        String getTitle();

        Integer getDays();

        String getBudget();

        String getPreference();

        String getLocale();

        AiRouteRecord.Status getStatus();

        Boolean getManuallySaved();

        String getErrorMessage();

        LocalDateTime getCreatedAt();

        LocalDateTime getUpdatedAt();
    }
}
