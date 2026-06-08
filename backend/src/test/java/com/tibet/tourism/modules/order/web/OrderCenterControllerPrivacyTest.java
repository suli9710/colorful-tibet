package com.tibet.tourism.modules.order.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.common.security.antibot.RiskAssessmentService;
import com.tibet.tourism.common.security.antibot.RiskResult;
import com.tibet.tourism.modules.order.application.OrderCenterService;
import com.tibet.tourism.modules.order.web.dto.CreateOrderRequest;
import com.tibet.tourism.modules.order.web.dto.PaymentCallbackRequest;
import com.tibet.tourism.modules.order.web.dto.RefundRequest;
import com.tibet.tourism.modules.user.domain.User;
import java.math.BigDecimal;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

class OrderCenterControllerPrivacyTest {

    private final OrderCenterService orderCenterService = mock(OrderCenterService.class);
    private final JwtAuthSupport jwtAuthSupport = mock(JwtAuthSupport.class);
    private final RiskAssessmentService riskAssessmentService = mock(RiskAssessmentService.class);
    private final OrderCenterController controller = new OrderCenterController(
            orderCenterService,
            jwtAuthSupport,
            riskAssessmentService);
    private final MockHttpServletRequest httpRequest = new MockHttpServletRequest();
    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(42L);
        when(jwtAuthSupport.resolveCurrentUser(httpRequest)).thenReturn(user);
        when(riskAssessmentService.assess(any(), any(), eq(42L), any(), any(), any()))
                .thenReturn(RiskResult.allow());
    }

    @Test
    void createOrderDoesNotExposeServiceExceptionMessage() {
        when(orderCenterService.createOrder(eq(user), any(CreateOrderRequest.class), any()))
                .thenThrow(new IllegalArgumentException("Payment amount mismatch transaction=txn-secret"));

        ResponseEntity<?> response = controller.createOrder(
                new CreateOrderRequest(), "idem-key", null, null, null, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertBodyError(response, "Order request could not be processed")
                .doesNotContain("txn-secret")
                .doesNotContain("Payment amount mismatch");
    }

    @Test
    void refundConflictDoesNotExposeServiceExceptionMessage() {
        when(orderCenterService.requestRefund(eq(user), eq(7L), any(RefundRequest.class)))
                .thenThrow(new IllegalStateException("Refund amount exceeds paid amount"));

        ResponseEntity<?> response = controller.requestRefund(7L, new RefundRequest(), httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertBodyError(response, "Order action is not available")
                .doesNotContain("Refund amount exceeds paid amount");
    }

    @Test
    void paymentCallbackDoesNotExposeServiceExceptionMessage() {
        PaymentCallbackRequest request = new PaymentCallbackRequest(
                "order-1", "txn-1", "mock", BigDecimal.TEN, "SUCCESS", "sig");
        when(orderCenterService.handleMockPaymentCallback(eq(user), eq(request)))
                .thenThrow(new IllegalArgumentException("Payment transaction number already exists"));

        ResponseEntity<?> response = controller.paymentCallback(
                request, null, null, null, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertBodyError(response, "Payment callback could not be processed")
                .doesNotContain("transaction number");
    }

    @Test
    void paymentCallbackRejectsAntibotChallengeDecision() {
        PaymentCallbackRequest request = new PaymentCallbackRequest(
                "order-1", "txn-1", "mock", BigDecimal.TEN, "SUCCESS", "sig");
        when(riskAssessmentService.assess(any(), any(), eq(42L), any(), any(), any()))
                .thenReturn(new RiskResult(0, 0, 0, 60, RiskResult.Decision.CHALLENGE));

        ResponseEntity<?> response = controller.paymentCallback(
                request, null, null, null, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertBodyError(response, null)
                .doesNotContain("Payment callback");

        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body).containsEntry("code", "ANTIBOT_CHALLENGE");
    }

    private static org.assertj.core.api.AbstractStringAssert<?> assertBodyError(
            ResponseEntity<?> response,
            String expected) {
        assertThat(response.getBody()).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();

        var assertion = assertThat(body.get("error"));
        if (expected != null) {
            assertion.isEqualTo(expected);
        }
        return assertion;
    }
}
