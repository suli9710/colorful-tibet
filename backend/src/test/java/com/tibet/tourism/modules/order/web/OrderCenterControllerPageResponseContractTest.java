package com.tibet.tourism.modules.order.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static com.tibet.tourism.modules.admin.application.AdminAuditTestSupport.passthroughAuditService;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.tibet.tourism.common.api.PageResponse;
import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.common.security.antibot.RiskAssessmentService;
import com.tibet.tourism.modules.order.application.OrderCenterService;
import com.tibet.tourism.modules.order.web.dto.OrderSummaryResponse;
import com.tibet.tourism.modules.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class OrderCenterControllerPageResponseContractTest {

    private final OrderCenterService orderCenterService = mock(OrderCenterService.class);
    private final JwtAuthSupport jwtAuthSupport = mock(JwtAuthSupport.class);
    private final RiskAssessmentService riskAssessmentService = mock(RiskAssessmentService.class);
    private final HttpServletRequest httpRequest = mock(HttpServletRequest.class);
    private final OrderCenterController controller = new OrderCenterController(
            orderCenterService,
            jwtAuthSupport,
            riskAssessmentService,
            passthroughAuditService());
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Test
    void myOrdersReturnsStablePageEnvelopeAndKeepsPaginationHeaders() throws Exception {
        User user = new User();
        user.setId(42L);
        PageRequest pageRequest = PageRequest.of(1, 2);
        when(jwtAuthSupport.resolveCurrentUser(httpRequest)).thenReturn(user);
        when(orderCenterService.getMyOrders(eq(user), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(orderSummary()), pageRequest, 5));

        ResponseEntity<?> response = controller.myOrders(pageRequest, httpRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getFirst("X-Page")).isEqualTo("1");
        assertThat(response.getHeaders().getFirst("X-Size")).isEqualTo("2");
        assertThat(response.getHeaders().getFirst("X-Total-Elements")).isEqualTo("5");
        assertThat(response.getHeaders().getFirst("X-Total-Pages")).isEqualTo("3");
        assertThat(response.getBody()).isInstanceOf(PageResponse.class);

        String json = objectMapper.writeValueAsString(response.getBody());
        assertThat(json)
                .contains("\"content\"")
                .contains("\"page\":1")
                .contains("\"size\":2")
                .contains("\"totalElements\":5")
                .contains("\"totalPages\":3")
                .contains("\"orderNo\":\"ORD-20260609-1\"")
                .doesNotContain("\"pageable\"")
                .doesNotContain("\"sort\"")
                .doesNotContain("\"number\"")
                .doesNotContain("\"customerName\"")
                .doesNotContain("\"customerPhone\"")
                .doesNotContain("\"customerNote\"")
                .doesNotContain("\"paymentTransactions\"")
                .doesNotContain("T***r")
                .doesNotContain("139****0000")
                .doesNotContain("\"raw-phone\"");
    }

    private static OrderSummaryResponse orderSummary() {
        return new OrderSummaryResponse(
                99L,
                "ORD-20260609-1",
                "CONFIRMED",
                "PAID",
                "CNY",
                "Lhasa hotel x 1",
                new BigDecimal("880.00"),
                BigDecimal.ZERO,
                new BigDecimal("880.00"),
                "LEGACY_HOTEL_BOOKING",
                100L,
                null,
                null,
                LocalDateTime.parse("2026-06-09T10:00:00"),
                LocalDateTime.parse("2026-06-09T10:05:00"),
                null,
                LocalDateTime.parse("2026-06-09T09:30:00"));
    }
}
