package com.tibet.tourism.modules.spot.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.modules.order.domain.Booking;
import com.tibet.tourism.modules.order.infra.BookingRepository;
import com.tibet.tourism.modules.user.infra.UserVisitHistoryRepository;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class CompanionInferenceServiceTest {

    @Mock private BookingRepository bookingRepository;
    @Mock private UserVisitHistoryRepository historyRepository;

    private CompanionInferenceService service;

    @BeforeEach
    void setUp() {
        service = new CompanionInferenceService();
        ReflectionTestUtils.setField(service, "bookingRepository", bookingRepository);
        ReflectionTestUtils.setField(service, "historyRepository", historyRepository);
    }

    @Test
    void inferenceUsesBoundedRecommendationHistoryAndBookings() {
        when(bookingRepository.findByUserId(eq(42L), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(new PageImpl<Booking>(Collections.emptyList()));
        when(historyRepository.findRecentByUserId(42L)).thenReturn(Collections.emptyList());

        assertEquals("ALONE", service.getCompanionType(42L));

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(bookingRepository).findByUserId(eq(42L), pageableCaptor.capture());
        assertEquals(50, pageableCaptor.getValue().getPageSize());
        verify(bookingRepository, never()).findByUserId(42L);
        verify(historyRepository, times(2)).findRecentByUserId(42L);
        verify(historyRepository, never()).findByUserId(42L);
    }
}
