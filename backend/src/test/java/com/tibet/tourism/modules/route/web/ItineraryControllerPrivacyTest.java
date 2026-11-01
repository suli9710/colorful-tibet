package com.tibet.tourism.modules.route.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.common.security.antibot.RiskAssessmentService;
import com.tibet.tourism.common.security.antibot.RiskResult;
import com.tibet.tourism.modules.route.application.ItineraryService;
import com.tibet.tourism.modules.route.web.dto.itinerary.BookItineraryItemRequest;
import com.tibet.tourism.modules.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class ItineraryControllerPrivacyTest {

    @Mock
    private ItineraryService itineraryService;
    @Mock
    private JwtAuthSupport jwtAuthSupport;
    @Mock
    private HttpServletRequest request;
    @Mock
    private RiskAssessmentService riskAssessmentService;

    private ItineraryController controller;

    @BeforeEach
    void setUp() {
        controller = new ItineraryController(itineraryService, jwtAuthSupport, riskAssessmentService);
    }

    private ResponseEntity<?> bookItemAllowingRisk(long itineraryId, long itemId) {
        when(riskAssessmentService.assess(any(), any(), any(), any(), any(), any()))
                .thenReturn(RiskResult.allow());
        return controller.bookItem(itineraryId, itemId, new BookItineraryItemRequest(), null, null, null, request);
    }

    @Test
    void getItineraryNotFoundDoesNotExposeRawServiceException(CapturedOutput output) {
        User user = authenticatedUser();
        String rawMessage = "itinerary id=11 owner=alice@example.com";
        when(itineraryService.getItinerary(user, 11L)).thenThrow(new NoSuchElementException(rawMessage));

        ResponseEntity<?> response = controller.get(11L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertBodyError(response, "Itinerary not found")
                .doesNotContain(rawMessage, "alice@example.com");
        assertThat(output).contains("type=NoSuchElementException", "messageHash=")
                .doesNotContain(rawMessage, "alice@example.com");
    }

    @Test
    void bookItemBadRequestDoesNotExposeRawServiceException(CapturedOutput output) {
        User user = authenticatedUser();
        String rawMessage = "hotel booking phone=13800138000 providerToken=raw-token";
        when(itineraryService.bookItem(eq(user), eq(11L), eq(22L), any(BookItineraryItemRequest.class)))
                .thenThrow(new IllegalArgumentException(rawMessage));

        ResponseEntity<?> response = bookItemAllowingRisk(11L, 22L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertBodyError(response, "Booking request could not be processed")
                .doesNotContain(rawMessage, "13800138000", "raw-token");
        assertThat(output).contains("type=IllegalArgumentException", "messageHash=")
                .doesNotContain(rawMessage, "13800138000", "raw-token");
    }

    @Test
    void bookItemForbiddenDoesNotExposeRawServiceException(CapturedOutput output) {
        User user = authenticatedUser();
        String rawMessage = "owner user alice@example.com cannot book item";
        when(itineraryService.bookItem(eq(user), eq(11L), eq(22L), any(BookItineraryItemRequest.class)))
                .thenThrow(new SecurityException(rawMessage));

        ResponseEntity<?> response = bookItemAllowingRisk(11L, 22L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertBodyError(response, "Itinerary action is not allowed")
                .doesNotContain(rawMessage, "alice@example.com");
        assertThat(output).contains("type=SecurityException", "messageHash=")
                .doesNotContain(rawMessage, "alice@example.com");
    }

    @Test
    void bookItemIsGatedByTheSameAntibotCheckAsTheDirectBookingEndpoints() {
        authenticatedUser();
        when(riskAssessmentService.assess(any(), any(), any(), any(), any(), any()))
                .thenReturn(new RiskResult(0.9, 0.9, 0.9, 0.9, RiskResult.Decision.BLOCK));

        ResponseEntity<?> response = controller.bookItem(
                11L, 22L, new BookItineraryItemRequest(), null, null, null, request);

        // Without this gate the itinerary endpoint was simply the unguarded way to create the same
        // spot and hotel bookings that /api/bookings and /api/hotel-bookings protect.
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertBodyError(response, "Security validation failed");
        verifyNoInteractions(itineraryService);
    }

    private User authenticatedUser() {
        User user = new User();
        user.setId(7L);
        user.setUsername("traveler");
        when(jwtAuthSupport.resolveCurrentUser(request)).thenReturn(user);
        return user;
    }

    private static org.assertj.core.api.AbstractStringAssert<?> assertBodyError(
            ResponseEntity<?> response,
            String expected) {
        assertThat(response.getBody()).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();

        return assertThat(body.get("error")).isEqualTo(expected);
    }
}
