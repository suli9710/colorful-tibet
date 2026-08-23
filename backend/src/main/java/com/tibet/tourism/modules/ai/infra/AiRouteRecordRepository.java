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

    /**
     * The record a cache hit should refresh instead of inserting a new one. Serving a cached route
     * used to persist a fresh full-content row on every request, so repeatedly asking for the same
     * itinerary grew the table without bound. Manually saved records are excluded so a user's kept
     * routes are never overwritten.
     */
    @Query("""
            SELECT r FROM AiRouteRecord r
            WHERE r.user.id = :userId
              AND r.jobId IS NULL
              AND r.days = :days
              AND r.budget = :budget
              AND r.preference = :preference
              AND r.locale = :locale
              AND (r.manuallySaved IS NULL OR r.manuallySaved = false)
            ORDER BY r.updatedAt DESC
            """)
    List<AiRouteRecord> findReusableCachedRecords(@Param("userId") Long userId,
                                                  @Param("days") Integer days,
                                                  @Param("budget") String budget,
                                                  @Param("preference") String preference,
                                                  @Param("locale") String locale,
                                                  Pageable pageable);

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
