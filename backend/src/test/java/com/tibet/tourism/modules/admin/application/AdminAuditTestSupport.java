package com.tibet.tourism.modules.admin.application;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

import java.util.function.Function;
import java.util.function.Supplier;
import org.springframework.http.ResponseEntity;

public final class AdminAuditTestSupport {

    private AdminAuditTestSupport() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static AdminAuditLogService passthroughAuditService() {
        AdminAuditLogService service = mock(AdminAuditLogService.class);
        lenient().when(service.capture(
                        anyString(), nullable(Long.class), anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> ((Supplier<ResponseEntity<?>>) invocation.getArgument(3)).get());
        lenient().when(service.captureCreated(
                        anyString(), anyString(), any(Supplier.class), any(Function.class)))
                .thenAnswer(invocation -> ((Supplier<ResponseEntity<?>>) invocation.getArgument(2)).get());
        lenient().when(service.captureIfAdmin(
                        anyString(), nullable(Long.class), anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> ((Supplier<ResponseEntity<?>>) invocation.getArgument(3)).get());
        return service;
    }
}
