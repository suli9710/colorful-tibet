package com.tibet.tourism.modules.user.infra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.modules.user.domain.UserVisitHistory;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

class UserVisitHistoryRepositoryRecommendationLimitTest {

    @Test
    void defaultRecentUserHistoryUsesRecommendationLimitAndRecentSort() {
        UserVisitHistoryRepository repository = mock(UserVisitHistoryRepository.class, CALLS_REAL_METHODS);
        when(repository.findRecentByUserId(eq(7L), any(Pageable.class))).thenReturn(List.of());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

        repository.findRecentByUserId(7L);

        verify(repository).findRecentByUserId(eq(7L), pageable.capture());
        assertEquals(UserVisitHistoryRepository.DEFAULT_RECOMMENDATION_HISTORY_LIMIT,
                pageable.getValue().getPageSize());
        assertSortsByRecentVisit(pageable.getValue().getSort());
    }

    @Test
    void explicitRecentUserHistoryLimitIsAppliedAtRepositoryBoundary() {
        UserVisitHistoryRepository repository = mock(UserVisitHistoryRepository.class, CALLS_REAL_METHODS);
        when(repository.findRecentByUserId(eq(7L), any(Pageable.class))).thenReturn(List.of());
        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);

        repository.findRecentByUserId(7L, 3);

        verify(repository).findRecentByUserId(eq(7L), pageable.capture());
        assertEquals(3, pageable.getValue().getPageSize());
        assertSortsByRecentVisit(pageable.getValue().getSort());
    }

    @Test
    void collectionRecentHistoryShortCircuitsEmptyInputs() {
        UserVisitHistoryRepository repository = mock(UserVisitHistoryRepository.class, CALLS_REAL_METHODS);

        List<UserVisitHistory> histories = repository.findRecentBySpotIdIn(Collections.emptyList());

        assertTrue(histories.isEmpty());
        verify(repository, never()).findRecentBySpotIdIn(any(), any(Pageable.class));
    }

    private static void assertSortsByRecentVisit(Sort sort) {
        Sort.Order visitDate = sort.getOrderFor("visitDate");
        Sort.Order id = sort.getOrderFor("id");
        assertEquals(Sort.Direction.DESC, visitDate.getDirection());
        assertEquals(Sort.Direction.DESC, id.getDirection());
    }
}
