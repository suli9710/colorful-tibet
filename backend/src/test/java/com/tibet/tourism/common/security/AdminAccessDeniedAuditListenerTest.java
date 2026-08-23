package com.tibet.tourism.common.security;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.security.AdminAccessDeniedAuditEvent.Category;
import com.tibet.tourism.common.security.AdminAccessDeniedAuditEvent.Source;
import com.tibet.tourism.modules.admin.application.AdminAuditLogService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AdminAccessDeniedAuditListenerTest {

    private AdminAuditLogService auditLogService;
    private AdminAccessDeniedAuditListener listener;

    @BeforeEach
    void setUp() {
        auditLogService = mock(AdminAuditLogService.class);
        listener = new AdminAccessDeniedAuditListener(auditLogService);
    }

    @Test
    void persistsDeniedOutcomeUsingOnlyStableEventLabels() {
        AdminAuditLogService.Attempt attempt = new AdminAuditLogService.Attempt(
                9L, 2L, "actor-ref", 72L, "target-ref", "hotel_booking_status_access_denied");
        when(auditLogService.begin(
                "hotel_booking", 72L, "hotel_booking_status_access_denied"))
                .thenReturn(attempt);

        listener.onAccessDenied(new AdminAccessDeniedAuditEvent(
                Category.HOTEL_BOOKING_STATUS, 72L, Source.METHOD_SECURITY));

        verify(auditLogService).complete(
                attempt,
                AdminAuditLogService.Result.DENIED,
                "method_security");
    }

    @Test
    void persistenceFailureNeverEscapesTheListener() {
        when(auditLogService.begin("admin_api", null, "admin_api_access_denied"))
                .thenThrow(new AdminAuditLogService.AuditUnavailableException());

        assertThatCode(() -> listener.onAccessDenied(new AdminAccessDeniedAuditEvent(
                Category.ADMIN_API, null, Source.SECURITY_FILTER)))
                .doesNotThrowAnyException();
    }
}
