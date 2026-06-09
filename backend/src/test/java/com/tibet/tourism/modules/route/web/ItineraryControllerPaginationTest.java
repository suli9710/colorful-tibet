package com.tibet.tourism.modules.route.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.modules.route.application.ItineraryService;
import com.tibet.tourism.modules.route.web.dto.itinerary.ItinerarySummaryResponse;
import com.tibet.tourism.modules.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ItineraryControllerPaginationTest {

    @Mock
    private ItineraryService itineraryService;
    @Mock
    private JwtAuthSupport jwtAuthSupport;
    @Mock
    private HttpServletRequest request;

    private ItineraryController controller;
    private User user;

    @BeforeEach
    void setUp() {
        controller = new ItineraryController(itineraryService, jwtAuthSupport);
        user = new User();
        user.setId(7L);
        user.setUsername("traveler");
        when(jwtAuthSupport.resolveCurrentUser(request)).thenReturn(user);
    }

    @Test
    void myItinerariesKeepsLegacyArrayBodyWithPageHeaders() {
        PageRequest pageable = PageRequest.of(1, 2);
        ItinerarySummaryResponse itinerary = itineraryResponse(11L);
        when(itineraryService.getMyItineraries(eq(user), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(itinerary), pageable, 5));

        ResponseEntity<List<ItinerarySummaryResponse>> response = controller.myItineraries(request, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsExactly(itinerary);
        assertThat(response.getHeaders().getFirst("X-Page")).isEqualTo("1");
        assertThat(response.getHeaders().getFirst("X-Size")).isEqualTo("2");
        assertThat(response.getHeaders().getFirst("X-Total-Elements")).isEqualTo("5");
        assertThat(response.getHeaders().getFirst("X-Total-Pages")).isEqualTo("3");
        verify(itineraryService).getMyItineraries(user, pageable);
    }

    private static ItinerarySummaryResponse itineraryResponse(Long id) {
        return new ItinerarySummaryResponse(
                id,
                null,
                "Lhasa itinerary",
                3,
                LocalDate.of(2026, 6, 1),
                "comfort",
                "cultural",
                "default",
                "standard",
                BigDecimal.valueOf(1200),
                "DRAFT",
                LocalDateTime.of(2026, 5, 1, 10, 0));
    }
}
