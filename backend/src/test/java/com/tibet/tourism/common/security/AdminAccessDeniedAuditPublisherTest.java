package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.tibet.tourism.common.security.AdminAccessDeniedAuditEvent.Category;
import com.tibet.tourism.common.security.AdminAccessDeniedAuditEvent.Source;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletRequest;

class AdminAccessDeniedAuditPublisherTest {

    private ApplicationEventPublisher eventPublisher;
    private AdminAccessDeniedAuditPublisher publisher;

    @BeforeEach
    void setUp() {
        eventPublisher = mock(ApplicationEventPublisher.class);
        publisher = new AdminAccessDeniedAuditPublisher(eventPublisher);
    }

    @Test
    void classifiesEveryAdministratorSecurityBoundaryWithStableLabels() {
        assertClassification("GET", "/api/admin/users/72", Category.ADMIN_API, null);
        assertClassification("POST", "/api/prices/update", Category.PRICE_ADMIN_API, null);
        assertClassification("DELETE", "/api/spots/admin/72", Category.SCENIC_SPOT_ADMIN_API, null);
        assertClassification("GET", "/api/spots/recommendations/debug", Category.RECOMMENDATION_DEBUG, null);
        assertClassification("PUT", "/api/hotel-bookings/72/status", Category.HOTEL_BOOKING_STATUS, 72L);
    }

    @Test
    void eventNeverContainsRawRequestData() {
        MockHttpServletRequest request = request("GET", "/api/admin/users/raw-path-secret");
        request.setQueryString("token=query-secret");
        request.addHeader("Authorization", "Bearer header-secret");
        request.setContent("{\"password\":\"body-secret\"}".getBytes(StandardCharsets.UTF_8));

        publisher.publish(request, Source.SECURITY_FILTER);

        AdminAccessDeniedAuditEvent event = captureEvent();
        assertThat(event.category()).isEqualTo(Category.ADMIN_API);
        assertThat(event.targetId()).isNull();
        assertThat(event.toString())
                .doesNotContain("raw-path-secret")
                .doesNotContain("query-secret")
                .doesNotContain("header-secret")
                .doesNotContain("body-secret");
    }

    @Test
    void skipsPiiRevealAndUnrelatedOrNonPutHotelStatusRequests() {
        publisher.publish(request("GET", "/api/hotel-bookings/72/pii"), Source.METHOD_SECURITY);
        publisher.publish(request("POST", "/api/hotel-bookings/72/status"), Source.METHOD_SECURITY);
        publisher.publish(request("GET", "/api/spots/72"), Source.SECURITY_FILTER);

        verifyNoInteractions(eventPublisher);
    }

    @Test
    void auditDispatchFailureNeverEscapes() {
        doThrow(new IllegalStateException("Authorization: Bearer raw-secret"))
                .when(eventPublisher).publishEvent(any(Object.class));

        assertThatCode(() -> publisher.publish(
                request("GET", "/api/admin/users"), Source.SECURITY_FILTER))
                .doesNotThrowAnyException();
    }

    private void assertClassification(String method, String path, Category category, Long targetId) {
        reset(eventPublisher);
        publisher.publish(request(method, path), Source.METHOD_SECURITY);

        AdminAccessDeniedAuditEvent event = captureEvent();
        assertThat(event.category()).isEqualTo(category);
        assertThat(event.targetId()).isEqualTo(targetId);
        assertThat(event.source()).isEqualTo(Source.METHOD_SECURITY);
    }

    private AdminAccessDeniedAuditEvent captureEvent() {
        ArgumentCaptor<Object> captor = ArgumentCaptor.forClass(Object.class);
        verify(eventPublisher).publishEvent(captor.capture());
        assertThat(captor.getValue()).isInstanceOf(AdminAccessDeniedAuditEvent.class);
        return (AdminAccessDeniedAuditEvent) captor.getValue();
    }

    private MockHttpServletRequest request(String method, String path) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setServletPath(path);
        return request;
    }
}
