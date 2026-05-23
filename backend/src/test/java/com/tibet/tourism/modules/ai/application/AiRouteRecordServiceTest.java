package com.tibet.tourism.modules.ai.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.error.BusinessException;
import com.tibet.tourism.common.error.ResourceNotFoundException;
import com.tibet.tourism.modules.ai.domain.AiRouteRecord;
import com.tibet.tourism.modules.ai.infra.AiRouteRecordRepository;
import com.tibet.tourism.modules.ai.web.dto.AiRouteRecordResponse;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.util.List;
import java.util.Optional;
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
        assertEquals("Lhasa route", record.getTitle());
        verify(routeRecordRepository).findFirstByUserIdAndJobIdOrderByUpdatedAtDesc(7L, "job-1");
    }

    @Test
    void saveForUserUsesOwnedLookupAndMarksPrivateSaved() {
        User user = user(7L);
        AiRouteRecord record = record(31L, user, "# Saved route");
        when(routeRecordRepository.findByIdAndUser(31L, user)).thenReturn(Optional.of(record));
        when(routeRecordRepository.save(record)).thenReturn(record);

        AiRouteRecordResponse response = service.saveForUser(31L, user);

        assertEquals(31L, response.id());
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
        when(routeRecordRepository.findByUserAndManuallySavedTrueOrderByUpdatedAtDesc(user))
                .thenReturn(List.of(record(1L, user, "# One")));

        List<AiRouteRecordResponse> saved = service.savedFor(user);

        assertEquals(1, saved.size());
        assertEquals(1L, saved.get(0).id());
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
