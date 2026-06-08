package com.tibet.tourism.modules.route.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.modules.route.application.TibetTravelKitService;
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
class TibetSpecialtyControllerPrivacyTest {

    @Mock
    private TibetTravelKitService travelKitService;
    @Mock
    private JwtAuthSupport jwtAuthSupport;
    @Mock
    private HttpServletRequest request;

    private TibetSpecialtyController controller;

    @BeforeEach
    void setUp() {
        controller = new TibetSpecialtyController(travelKitService, jwtAuthSupport);
    }

    @Test
    void cultureTipsDoesNotExposeRawFilterException(CapturedOutput output) {
        String rawMessage = "scene=alice@example.com token=culture-secret";
        when(travelKitService.getCultureTips(rawMessage)).thenThrow(new IllegalArgumentException(rawMessage));

        ResponseEntity<?> response = controller.cultureTips(rawMessage);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertBodyError(response, "Specialty request could not be processed")
                .doesNotContain(rawMessage, "alice@example.com", "culture-secret");
        assertThat(output).contains("type=IllegalArgumentException", "messageHash=")
                .doesNotContain(rawMessage, "alice@example.com", "culture-secret");
    }

    @Test
    void phrasebookDoesNotExposeRawFilterException(CapturedOutput output) {
        String rawMessage = "category contains phone=13800138000";
        when(travelKitService.getPhrasebook(rawMessage)).thenThrow(new IllegalArgumentException(rawMessage));

        ResponseEntity<?> response = controller.phrasebook(rawMessage);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertBodyError(response, "Specialty request could not be processed")
                .doesNotContain(rawMessage, "13800138000");
        assertThat(output).contains("type=IllegalArgumentException", "messageHash=")
                .doesNotContain(rawMessage, "13800138000");
    }

    @Test
    void sustainableOptionsDoesNotExposeRawFilterException(CapturedOutput output) {
        String rawMessage = "region tenant=internal-ledger";
        when(travelKitService.getSustainableOptions(rawMessage)).thenThrow(new IllegalArgumentException(rawMessage));

        ResponseEntity<?> response = controller.sustainableOptions(rawMessage);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertBodyError(response, "Specialty request could not be processed")
                .doesNotContain(rawMessage, "internal-ledger");
        assertThat(output).contains("type=IllegalArgumentException", "messageHash=")
                .doesNotContain(rawMessage, "internal-ledger");
    }

    @Test
    void travelKitNotFoundDoesNotExposeRawServiceException(CapturedOutput output) {
        User user = user();
        String rawMessage = "itinerary belongs to alice@example.com";
        when(jwtAuthSupport.resolveCurrentUser(request)).thenReturn(user);
        when(travelKitService.buildTravelKit(user, 55L)).thenThrow(new NoSuchElementException(rawMessage));

        ResponseEntity<?> response = controller.travelKit(55L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertBodyError(response, "Itinerary not found")
                .doesNotContain(rawMessage, "alice@example.com");
        assertThat(output).contains("type=NoSuchElementException", "messageHash=")
                .doesNotContain(rawMessage, "alice@example.com");
    }

    private static User user() {
        User user = new User();
        user.setId(7L);
        user.setUsername("traveler");
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
