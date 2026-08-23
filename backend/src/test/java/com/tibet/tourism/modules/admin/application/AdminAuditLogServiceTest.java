package com.tibet.tourism.modules.admin.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.tibet.tourism.common.security.CacheKeyHasher;
import com.tibet.tourism.modules.admin.domain.AdminAuditLog;
import com.tibet.tourism.modules.admin.infra.AdminAuditLogRepository;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class AdminAuditLogServiceTest {

    @Mock
    private AdminAuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlatformTransactionManager transactionManager;

    @Mock
    private TransactionStatus transactionStatus;

    private CacheKeyHasher cacheKeyHasher;
    private AdminAuditLogService service;
    private final AtomicLong auditSequence = new AtomicLong();

    @BeforeEach
    void setUp() {
        cacheKeyHasher = new CacheKeyHasher("independent-test-hmac-secret");
        service = new AdminAuditLogService(
                auditLogRepository, userRepository, cacheKeyHasher, transactionManager);
        lenient().when(transactionManager.getTransaction(any(TransactionDefinition.class)))
                .thenReturn(transactionStatus);
        lenient().when(auditLogRepository.saveAndFlush(any(AdminAuditLog.class)))
                .thenAnswer(invocation -> {
                    AdminAuditLog log = invocation.getArgument(0);
                    log.setId(auditSequence.incrementAndGet());
                    return log;
                });
        lenient().when(auditLogRepository.findById(anyLong()))
                .thenAnswer(invocation -> Optional.ofNullable(savedAttempt(invocation.getArgument(0))));

        User actor = new User();
        actor.setId(42L);
        actor.setUsername("administrator");
        lenient().when(userRepository.findByUsername("administrator")).thenReturn(Optional.of(actor));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "administrator",
                        "ignored",
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void capturePersistsOnlyCodeOwnedLabelsAndHmacReferences() {
        ResponseEntity<String> response = ResponseEntity.ok("business-response");

        ResponseEntity<String> actual = service.capture(
                "news", 7L, "news_update", () -> response);

        assertThat(actual).isSameAs(response);
        AdminAuditLog log = savedLog();
        assertThat(log.getActorId()).isEqualTo(42L);
        assertThat(log.getActorRef())
                .isEqualTo(cacheKeyHasher.cacheKey("admin-audit-actor", "administrator"))
                .doesNotContain("administrator");
        assertThat(log.getTargetId()).isEqualTo(7L);
        assertThat(log.getTargetRef())
                .isEqualTo(cacheKeyHasher.cacheKey("admin-audit-target", "news:7"))
                .doesNotContain("news:7");
        assertThat(log.getAction()).isEqualTo("news_update");
        assertThat(log.getResult()).isEqualTo("success");
        assertThat(log.getReason()).isEqualTo("http_200");
        verify(transactionManager, times(2)).commit(transactionStatus);
    }

    @Test
    void captureCreatedCommitsNewAttemptBeforeBindingSuccessfulCreatedEntity() {
        AtomicReference<Long> targetIdDuringOperation = new AtomicReference<>();
        AtomicReference<String> targetRefDuringOperation = new AtomicReference<>();

        ResponseEntity<CreatedResponse> response = service.captureCreated(
                "news",
                "news_create",
                () -> {
                    AdminAuditLog attempt = insertedAttempt();
                    targetIdDuringOperation.set(attempt.getTargetId());
                    targetRefDuringOperation.set(attempt.getTargetRef());
                    return ResponseEntity.ok(new CreatedResponse(73L));
                },
                CreatedResponse::id);

        assertThat(response.getBody()).isEqualTo(new CreatedResponse(73L));
        assertThat(targetIdDuringOperation.get()).isNull();
        assertThat(targetRefDuringOperation.get())
                .isEqualTo(cacheKeyHasher.cacheKey("admin-audit-target", "news:new"));

        AdminAuditLog completed = savedLog();
        assertThat(completed.getTargetId()).isEqualTo(73L);
        assertThat(completed.getTargetRef())
                .isEqualTo(cacheKeyHasher.cacheKey("admin-audit-target", "news:73"))
                .doesNotContain("news:73");
        assertThat(completed.getResult()).isEqualTo("success");
        assertThat(completed.getReason()).isEqualTo("http_200");
        verify(transactionManager, times(2)).commit(transactionStatus);
    }

    @Test
    void captureCreatedDoesNotExtractOrBindTargetForNonSuccessResponse() {
        AtomicBoolean extractorCalled = new AtomicBoolean();

        ResponseEntity<CreatedResponse> response = service.captureCreated(
                "news",
                "news_create",
                () -> ResponseEntity.status(409).body(new CreatedResponse(73L)),
                body -> {
                    extractorCalled.set(true);
                    return body.id();
                });

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        assertThat(extractorCalled).isFalse();
        AdminAuditLog completed = savedLog();
        assertThat(completed.getTargetId()).isNull();
        assertThat(completed.getTargetRef())
                .isEqualTo(cacheKeyHasher.cacheKey("admin-audit-target", "news:new"));
        assertThat(completed.getResult()).isEqualTo("failure");
        assertThat(completed.getReason()).isEqualTo("http_409");
    }

    @Test
    void captureCreatedDoesNotExtractOrBindTargetWhenOperationThrows() {
        AtomicBoolean extractorCalled = new AtomicBoolean();
        IllegalArgumentException failure = new IllegalArgumentException("private-request-material");

        assertThatThrownBy(() -> service.captureCreated(
                "news",
                "news_create",
                () -> {
                    throw failure;
                },
                (CreatedResponse body) -> {
                    extractorCalled.set(true);
                    return body.id();
                }))
                .isSameAs(failure);

        assertThat(extractorCalled).isFalse();
        AdminAuditLog completed = savedLog();
        assertThat(completed.getTargetId()).isNull();
        assertThat(completed.getTargetRef())
                .isEqualTo(cacheKeyHasher.cacheKey("admin-audit-target", "news:new"));
        assertThat(completed.getResult()).isEqualTo("failure");
        assertThat(completed.getReason()).isEqualTo("exception_IllegalArgumentException");
        assertThat(completed.getReason()).doesNotContain("private-request-material");
    }

    @Test
    void captureCreatedKeepsNewTargetWhenBusinessTransactionRollsBack() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            ResponseEntity<CreatedResponse> response = service.captureCreated(
                    "heritage_item",
                    "heritage_item_create",
                    () -> ResponseEntity.ok(new CreatedResponse(81L)),
                    CreatedResponse::id);

            assertThat(response.getBody()).isEqualTo(new CreatedResponse(81L));
            verify(auditLogRepository, never()).findById(anyLong());

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(synchronization -> synchronization.afterCompletion(
                            TransactionSynchronization.STATUS_ROLLED_BACK));

            AdminAuditLog completed = savedLog();
            assertThat(completed.getTargetId()).isNull();
            assertThat(completed.getTargetRef())
                    .isEqualTo(cacheKeyHasher.cacheKey("admin-audit-target", "heritage_item:new"));
            assertThat(completed.getResult()).isEqualTo("failure");
            assertThat(completed.getReason()).isEqualTo("transaction_rollback");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void captureCreatedExtractorFailureKeepsAttemptAndBusinessResponse() {
        ResponseEntity<CreatedResponse> response = service.captureCreated(
                "carousel",
                "carousel_create",
                () -> ResponseEntity.ok(new CreatedResponse(91L)),
                ignored -> {
                    throw new IllegalStateException("request-body-must-not-be-persisted");
                });

        assertThat(response.getBody()).isEqualTo(new CreatedResponse(91L));
        AdminAuditLog attempt = insertedAttempt();
        assertThat(attempt.getTargetId()).isNull();
        assertThat(attempt.getTargetRef())
                .isEqualTo(cacheKeyHasher.cacheKey("admin-audit-target", "carousel:new"));
        assertThat(attempt.getResult()).isEqualTo("attempt");
        assertThat(attempt.getReason()).isEqualTo("started");
        verify(auditLogRepository, never()).findById(anyLong());
    }

    @Test
    void captureCreatedOutcomeUpdateFailureKeepsNewAttemptAndBusinessResponse() {
        when(auditLogRepository.findById(anyLong()))
                .thenThrow(new IllegalStateException("jdbc:secret-database"));

        ResponseEntity<CreatedResponse> response = service.captureCreated(
                "carousel",
                "carousel_create",
                () -> ResponseEntity.ok(new CreatedResponse(92L)),
                CreatedResponse::id);

        assertThat(response.getBody()).isEqualTo(new CreatedResponse(92L));
        AdminAuditLog attempt = insertedAttempt();
        assertThat(attempt.getTargetId()).isNull();
        assertThat(attempt.getTargetRef())
                .isEqualTo(cacheKeyHasher.cacheKey("admin-audit-target", "carousel:new"));
        assertThat(attempt.getResult()).isEqualTo("attempt");
        assertThat(attempt.getReason()).isEqualTo("started");
        verify(auditLogRepository, never()).delete(any(AdminAuditLog.class));
    }

    @Test
    void captureRecordsExceptionTypeWithoutPersistingExceptionMessage() {
        IllegalStateException failure = new IllegalStateException(
                "password=top-secret&token=raw-token");

        assertThatThrownBy(() -> service.capture(
                "news", 8L, "news_delete", () -> {
                    throw failure;
                }))
                .isSameAs(failure);

        AdminAuditLog log = savedLog();
        assertThat(log.getResult()).isEqualTo("failure");
        assertThat(log.getReason()).isEqualTo("exception_IllegalStateException");
        assertThat(log.getReason()).doesNotContain("top-secret", "raw-token");
    }

    @Test
    void auditDatabaseFailurePreventsBusinessOperation() {
        AtomicBoolean operationCalled = new AtomicBoolean();
        when(auditLogRepository.saveAndFlush(any(AdminAuditLog.class)))
                .thenThrow(new IllegalStateException("jdbc:secret-database"));

        assertThatThrownBy(() -> service.capture(
                "carousel", null, "carousel_create", () -> {
                    operationCalled.set(true);
                    return ResponseEntity.ok("created");
                }))
                .isInstanceOf(AdminAuditLogService.AuditUnavailableException.class)
                .hasMessage("Administrator audit trail is unavailable");

        assertThat(operationCalled).isFalse();
        verify(transactionManager).rollback(transactionStatus);
    }

    @Test
    void attemptIsCommittedBeforeBusinessOperationStarts() {
        ResponseEntity<String> response = ResponseEntity.ok("created");

        service.capture("carousel", null, "carousel_create", () -> {
            verify(transactionManager).commit(transactionStatus);
            return response;
        });

        InOrder order = inOrder(auditLogRepository);
        order.verify(auditLogRepository).saveAndFlush(any(AdminAuditLog.class));
        order.verify(auditLogRepository).findById(anyLong());
    }

    @Test
    void outcomeUpdateFailureKeepsCommittedAttemptAndBusinessResponse() {
        when(auditLogRepository.findById(anyLong()))
                .thenThrow(new IllegalStateException("jdbc:secret-database"));

        ResponseEntity<String> response = service.capture(
                "carousel", null, "carousel_create", () -> ResponseEntity.ok("created"));

        assertThat(response.getBody()).isEqualTo("created");
        AdminAuditLog attempt = insertedAttempt();
        assertThat(attempt.getResult()).isEqualTo("attempt");
        assertThat(attempt.getReason()).isEqualTo("started");
        verify(auditLogRepository, never()).delete(any(AdminAuditLog.class));
    }

    @Test
    void nonSuccessResponseIsClassifiedWithoutCopyingResponseBody() {
        ResponseEntity<String> response = service.capture(
                "hotel", 9L, "hotel_delete",
                () -> ResponseEntity.status(409).body("contains-private-booking-details"));

        assertThat(response.getStatusCode().value()).isEqualTo(409);
        AdminAuditLog log = savedLog();
        assertThat(log.getResult()).isEqualTo("failure");
        assertThat(log.getReason()).isEqualTo("http_409");
        assertThat(log.getReason()).doesNotContain("private-booking-details");
    }

    @Test
    void transactionRollbackConvertsTentativeSuccessToFailure() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            ResponseEntity<String> response = service.capture(
                    "heritage_item", 11L, "heritage_item_update",
                    () -> ResponseEntity.ok("updated"));

            assertThat(response.getBody()).isEqualTo("updated");
            verify(auditLogRepository).saveAndFlush(any(AdminAuditLog.class));
            verify(auditLogRepository, never()).findById(anyLong());

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(synchronization -> synchronization.afterCompletion(
                            TransactionSynchronization.STATUS_ROLLED_BACK));

            AdminAuditLog log = savedLog();
            assertThat(log.getResult()).isEqualTo("failure");
            assertThat(log.getReason()).isEqualTo("transaction_rollback");
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    @Test
    void mixedEndpointBypassesAdministratorAuditForOrdinaryUser() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        "traveler",
                        "ignored",
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        ResponseEntity<String> response = service.captureIfAdmin(
                "hotel_booking", 21L, "hotel_booking_cancel",
                () -> ResponseEntity.ok("cancelled"));

        assertThat(response.getBody()).isEqualTo("cancelled");
        verifyNoInteractions(auditLogRepository);
    }

    @Test
    void mixedEndpointAuditsAdministratorOperation() {
        ResponseEntity<Void> response = service.captureIfAdmin(
                "hotel_booking", 22L, "hotel_booking_permanent_delete",
                () -> ResponseEntity.noContent().build());

        assertThat(response.getStatusCode().value()).isEqualTo(204);
        AdminAuditLog log = savedLog();
        assertThat(log.getTargetId()).isEqualTo(22L);
        assertThat(log.getAction()).isEqualTo("hotel_booking_permanent_delete");
        assertThat(log.getResult()).isEqualTo("success");
        assertThat(log.getReason()).isEqualTo("http_204");
    }

    private AdminAuditLog savedLog() {
        AdminAuditLog log = insertedAttempt();
        assertThat(log.getId()).isNotNull();
        return log;
    }

    private AdminAuditLog insertedAttempt() {
        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        verify(auditLogRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private AdminAuditLog savedAttempt(Long id) {
        ArgumentCaptor<AdminAuditLog> captor = ArgumentCaptor.forClass(AdminAuditLog.class);
        try {
            verify(auditLogRepository).saveAndFlush(captor.capture());
        } catch (AssertionError ignored) {
            return null;
        }
        AdminAuditLog log = captor.getValue();
        return id != null && id.equals(log.getId()) ? log : null;
    }

    private record CreatedResponse(Long id) {
    }
}
