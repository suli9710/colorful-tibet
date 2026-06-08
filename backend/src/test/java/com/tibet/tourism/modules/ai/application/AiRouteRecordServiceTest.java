package com.tibet.tourism.modules.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.error.BusinessException;
import com.tibet.tourism.common.error.ResourceNotFoundException;
import com.tibet.tourism.modules.ai.domain.AiRouteRecord;
import com.tibet.tourism.modules.ai.infra.AiRouteRecordRepository;
import com.tibet.tourism.modules.ai.web.dto.AiRouteRecordResponse;
import com.tibet.tourism.modules.ai.web.dto.AiRouteRecordSummaryResponse;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiRouteRecordServiceTest {

    @Mock
    private AiRouteRecordRepository routeRecordRepository;

    @Mock
    private UserRepository userRepository;

    private AiRouteRecordService service;

    @BeforeEach
    void setUp() {
        service = new AiRouteRecordService(routeRecordRepository, userRepository);
    }

    @Test
    void completedRouteIsStoredForCurrentUserOnly() {
        User user = user(7L);
        when(userRepository.getReferenceById(7L)).thenReturn(user);
        when(routeRecordRepository.findFirstByUserIdAndJobIdOrderByUpdatedAtDesc(7L, "job-1"))
                .thenReturn(Optional.empty());
        when(routeRecordRepository.save(any(AiRouteRecord.class))).thenAnswer(invocation -> {
            AiRouteRecord record = invocation.getArgument(0);
            record.setId(55L);
            return record;
        });

        AiRouteRecord record = service.recordCompletedRoute(
                user, "job-1", 4, "comfort", "natural", "zh", "# Lhasa route\n\ncontent");

        assertEquals(55L, record.getId());
        assertEquals(user, record.getUser());
        assertEquals(AiRouteRecord.Status.COMPLETED, record.getStatus());
        assertEquals("job-1", record.getJobId());
        assertEquals("Lhasa route", record.getTitle());
        verify(routeRecordRepository).findFirstByUserIdAndJobIdOrderByUpdatedAtDesc(7L, "job-1");
    }

    @Test
    void failedRouteRetainsStoredJobIdAfterUsingJobLookup() {
        User user = user(7L);
        AiRouteRecord record = record(44L, user, "# Partial route");
        record.setStatus(AiRouteRecord.Status.RUNNING);
        record.setJobId("job-2");

        when(userRepository.getReferenceById(7L)).thenReturn(user);
        when(routeRecordRepository.findFirstByUserIdAndJobIdOrderByUpdatedAtDesc(7L, "job-2"))
                .thenReturn(Optional.of(record));
        when(routeRecordRepository.save(record)).thenReturn(record);

        service.recordFailedRoute(user, "job-2", 4, "comfort", "natural", "zh", "failed");

        assertEquals(AiRouteRecord.Status.FAILED, record.getStatus());
        assertEquals("job-2", record.getJobId());
        verify(routeRecordRepository).findFirstByUserIdAndJobIdOrderByUpdatedAtDesc(7L, "job-2");
    }

    @Test
    void latestRunningRecordExposesJobIdForSseReconnect() {
        User user = user(7L);
        AiRouteRecord record = record(32L, user, "# Partial route");
        record.setStatus(AiRouteRecord.Status.RUNNING);
        record.setJobId("job-running");
        when(routeRecordRepository.findFirstByUserOrderByUpdatedAtDesc(user)).thenReturn(Optional.of(record));

        Optional<AiRouteRecordResponse> response = service.latestFor(user);

        assertTrue(response.isPresent());
        assertEquals("job-running", response.get().jobId());
    }

    @Test
    void latestCompletedRecordDoesNotExposeJobId() {
        User user = user(7L);
        AiRouteRecord record = record(33L, user, "# Done route");
        record.setJobId("legacy-completed-job");
        when(routeRecordRepository.findFirstByUserOrderByUpdatedAtDesc(user)).thenReturn(Optional.of(record));

        Optional<AiRouteRecordResponse> response = service.latestFor(user);

        assertTrue(response.isPresent());
        assertNull(response.get().jobId());
    }

    @Test
    void latestFailedRecordDoesNotExposeJobId() {
        User user = user(7L);
        AiRouteRecord record = record(34L, user, "# Failed route");
        record.setStatus(AiRouteRecord.Status.FAILED);
        record.setJobId("legacy-failed-job");
        when(routeRecordRepository.findFirstByUserOrderByUpdatedAtDesc(user)).thenReturn(Optional.of(record));

        Optional<AiRouteRecordResponse> response = service.latestFor(user);

        assertTrue(response.isPresent());
        assertNull(response.get().jobId());
    }

    @Test
    void staleRunningCleanupMarksRecordsFailedAndRetainsJobIds() {
        User user = user(7L);
        AiRouteRecord titledRecord = record(41L, user, "# Old route\n\npartial");
        titledRecord.setStatus(AiRouteRecord.Status.RUNNING);
        titledRecord.setJobId("job-old");
        titledRecord.setUpdatedAt(LocalDateTime.now().minusHours(3));
        AiRouteRecord emptyRecord = record(42L, user, "");
        emptyRecord.setStatus(AiRouteRecord.Status.RUNNING);
        emptyRecord.setJobId("job-empty");
        emptyRecord.setUpdatedAt(LocalDateTime.now().minusHours(4));

        when(routeRecordRepository.findByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(
                eq(AiRouteRecord.Status.RUNNING), any(LocalDateTime.class)))
                .thenReturn(List.of(titledRecord, emptyRecord));

        int recovered = service.failStaleRunningRecords(Duration.ofHours(2));

        assertEquals(2, recovered);
        assertEquals(AiRouteRecord.Status.FAILED, titledRecord.getStatus());
        assertEquals("job-old", titledRecord.getJobId());
        assertEquals("Old route", titledRecord.getTitle());
        assertTrue(titledRecord.getErrorMessage().contains("recovery window"));
        assertEquals(AiRouteRecord.Status.FAILED, emptyRecord.getStatus());
        assertEquals("job-empty", emptyRecord.getJobId());
        assertEquals(AiRouteRecord.defaultTitle(emptyRecord.getDays()), emptyRecord.getTitle());
        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(routeRecordRepository).findByStatusAndUpdatedAtBeforeOrderByUpdatedAtAsc(
                eq(AiRouteRecord.Status.RUNNING), cutoff.capture());
        assertTrue(cutoff.getValue().isBefore(LocalDateTime.now().minusMinutes(119)));
        verify(routeRecordRepository).saveAll(List.of(titledRecord, emptyRecord));
    }

    @Test
    void saveForUserUsesOwnedLookupAndMarksPrivateSaved() {
        User user = user(7L);
        AiRouteRecord record = record(31L, user, "# Saved route");
        record.setJobId("legacy-saved-job");
        when(routeRecordRepository.findByIdAndUser(31L, user)).thenReturn(Optional.of(record));
        when(routeRecordRepository.save(record)).thenReturn(record);

        AiRouteRecordResponse response = service.saveForUser(31L, user);

        assertEquals(31L, response.id());
        assertNull(response.jobId());
        assertTrue(response.manuallySaved());
        verify(routeRecordRepository).findByIdAndUser(31L, user);
    }

    @Test
    void saveForUserDoesNotRevealOtherUsersRecords() {
        User user = user(7L);
        when(routeRecordRepository.findByIdAndUser(99L, user)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.saveForUser(99L, user));
    }

    @Test
    void emptyContentCannotBeSavedToPersonalHistory() {
        User user = user(7L);
        AiRouteRecord record = record(31L, user, " ");
        when(routeRecordRepository.findByIdAndUser(31L, user)).thenReturn(Optional.of(record));

        assertThrows(BusinessException.class, () -> service.saveForUser(31L, user));
    }

    @Test
    void savedListIsScopedToCurrentUser() {
        User user = user(7L);
        AiRouteRecordRepository.AiRouteRecordSummaryProjection record = summaryProjection(1L, "Route");
        when(routeRecordRepository.findByUserAndManuallySavedTrue(eq(user), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(
                        List.of(record),
                        invocation.getArgument(1),
                        1));

        Page<AiRouteRecordSummaryResponse> saved = service.savedFor(user, PageRequest.of(0, 20));

        assertEquals(1, saved.getContent().size());
        assertEquals(1L, saved.getContent().get(0).id());
        verify(routeRecordRepository).findByUserAndManuallySavedTrue(eq(user), any(Pageable.class));
    }

    @Test
    void savedListUsesBoundedStablePageableAndMapsSummaryProjection() {
        User user = user(7L);
        when(routeRecordRepository.findByUserAndManuallySavedTrue(eq(user), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(
                        List.of(summaryProjection(2L, "Saved Route")),
                        invocation.getArgument(1),
                        200));

        Page<AiRouteRecordSummaryResponse> saved = service.savedFor(
                user,
                PageRequest.of(2, 500, Sort.by(Sort.Direction.DESC, "content")));

        assertEquals(1, saved.getContent().size());
        assertEquals(200, saved.getTotalElements());

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(routeRecordRepository).findByUserAndManuallySavedTrue(eq(user), pageableCaptor.capture());

        Pageable safePageable = pageableCaptor.getValue();
        assertEquals(2, safePageable.getPageNumber());
        assertEquals(50, safePageable.getPageSize());
        assertNull(safePageable.getSort().getOrderFor("content"));
        assertEquals(Sort.Direction.DESC, safePageable.getSort().getOrderFor("updatedAt").getDirection());
        assertEquals(Sort.Direction.DESC, safePageable.getSort().getOrderFor("id").getDirection());
    }

    @Test
    void savedListDefaultsToFirstTwentyWhenPageableIsMissing() {
        User user = user(7L);
        when(routeRecordRepository.findByUserAndManuallySavedTrue(eq(user), any(Pageable.class)))
                .thenAnswer(invocation -> new PageImpl<>(
                        List.of(summaryProjection(3L, "Default Route")),
                        invocation.getArgument(1),
                        1));

        service.savedFor(user, null);

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(routeRecordRepository).findByUserAndManuallySavedTrue(eq(user), pageableCaptor.capture());

        Pageable safePageable = pageableCaptor.getValue();
        assertEquals(0, safePageable.getPageNumber());
        assertEquals(20, safePageable.getPageSize());
        assertEquals(Sort.Direction.DESC, safePageable.getSort().getOrderFor("updatedAt").getDirection());
        assertEquals(Sort.Direction.DESC, safePageable.getSort().getOrderFor("id").getDirection());
    }

    @Test
    void savedDetailReturnsFullContentForOwnedSavedRecord() {
        User user = user(7L);
        AiRouteRecord record = record(31L, user, "# Saved route\n\nFull content");
        when(routeRecordRepository.findByIdAndUserAndManuallySavedTrue(31L, user))
                .thenReturn(Optional.of(record));

        AiRouteRecordResponse response = service.savedDetailFor(31L, user);

        assertEquals(31L, response.id());
        assertEquals("# Saved route\n\nFull content", response.content());
        verify(routeRecordRepository).findByIdAndUserAndManuallySavedTrue(31L, user);
    }

    private User user(Long id) {
        User user = new User();
        user.setId(id);
        user.setUsername("user-" + id);
        return user;
    }

    private AiRouteRecord record(Long id, User user, String content) {
        AiRouteRecord record = new AiRouteRecord();
        record.setId(id);
        record.setUser(user);
        record.setTitle("Route");
        record.setContent(content);
        record.setDays(5);
        record.setBudget("comfort");
        record.setPreference("natural");
        record.setLocale("zh");
        record.setStatus(AiRouteRecord.Status.COMPLETED);
        return record;
    }

    private AiRouteRecordRepository.AiRouteRecordSummaryProjection summaryProjection(Long id, String title) {
        LocalDateTime now = LocalDateTime.of(2026, 6, 8, 12, 0);
        return new AiRouteRecordRepository.AiRouteRecordSummaryProjection() {
            @Override
            public Long getId() {
                return id;
            }

            @Override
            public String getTitle() {
                return title;
            }

            @Override
            public Integer getDays() {
                return 5;
            }

            @Override
            public String getBudget() {
                return "comfort";
            }

            @Override
            public String getPreference() {
                return "natural";
            }

            @Override
            public String getLocale() {
                return "zh";
            }

            @Override
            public AiRouteRecord.Status getStatus() {
                return AiRouteRecord.Status.COMPLETED;
            }

            @Override
            public Boolean getManuallySaved() {
                return true;
            }

            @Override
            public String getErrorMessage() {
                return null;
            }

            @Override
            public LocalDateTime getCreatedAt() {
                return now.minusDays(1);
            }

            @Override
            public LocalDateTime getUpdatedAt() {
                return now;
            }
        };
    }
}
