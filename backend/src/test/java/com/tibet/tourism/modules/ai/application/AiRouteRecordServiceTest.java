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
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
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
        assertNull(record.getJobId());
        assertEquals("Lhasa route", record.getTitle());
        verify(routeRecordRepository).findFirstByUserIdAndJobIdOrderByUpdatedAtDesc(7L, "job-1");
    }

    @Test
    void failedRouteClearsStoredJobIdAfterUsingJobLookup() {
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
        assertNull(record.getJobId());
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
    void staleRunningCleanupMarksRecordsFailedAndClearsJobIds() {
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
        assertNull(titledRecord.getJobId());
        assertEquals("Old route", titledRecord.getTitle());
        assertTrue(titledRecord.getErrorMessage().contains("recovery window"));
        assertEquals(AiRouteRecord.Status.FAILED, emptyRecord.getStatus());
        assertNull(emptyRecord.getJobId());
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
        AiRouteRecord record = record(1L, user, "# One");
        record.setJobId("legacy-saved-job");
        when(routeRecordRepository.findByUserAndManuallySavedTrueOrderByUpdatedAtDesc(user))
                .thenReturn(List.of(record));

        List<AiRouteRecordResponse> saved = service.savedFor(user);

        assertEquals(1, saved.size());
        assertEquals(1L, saved.get(0).id());
        assertNull(saved.get(0).jobId());
        verify(routeRecordRepository).findByUserAndManuallySavedTrueOrderByUpdatedAtDesc(user);
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
}
