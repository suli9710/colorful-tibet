package com.tibet.tourism.modules.admin.application;

import com.tibet.tourism.common.security.CacheKeyHasher;
import com.tibet.tourism.common.security.SensitiveLogSanitizer;
import com.tibet.tourism.modules.admin.domain.AdminAuditLog;
import com.tibet.tourism.modules.admin.infra.AdminAuditLogRepository;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * Persists administrator mutations without coupling the audit row to the business transaction.
 * A committed {@code attempt/started} row is required before the mutation is allowed to run. The
 * final outcome is then applied in another transaction; if that update fails, the original attempt
 * remains as a durable indication that the operation may have run.
 *
 * <p>Callers supply only stable, code-owned labels and an optional numeric target id; request
 * bodies, query strings and raw paths never enter this service.</p>
 */
@Service
public class AdminAuditLogService {

    private static final Logger logger = LoggerFactory.getLogger(AdminAuditLogService.class);
    private static final String ANONYMOUS_ACTOR_REF = "user#anonymous";
    private static final String ATTEMPT_RESULT = "attempt";
    private static final String ATTEMPT_REASON = "started";
    private static final int MAX_LABEL_LENGTH = 64;
    private static final int MAX_ROLE_LENGTH = 32;

    private final AdminAuditLogRepository auditLogRepository;
    private final UserRepository userRepository;
    private final CacheKeyHasher cacheKeyHasher;
    private final TransactionTemplate requiresNewTransaction;

    public AdminAuditLogService(AdminAuditLogRepository auditLogRepository,
                                UserRepository userRepository,
                                CacheKeyHasher cacheKeyHasher,
                                PlatformTransactionManager transactionManager) {
        this.auditLogRepository = auditLogRepository;
        this.userRepository = userRepository;
        this.cacheKeyHasher = cacheKeyHasher;
        this.requiresNewTransaction = new TransactionTemplate(transactionManager);
        this.requiresNewTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public enum Result {
        SUCCESS("success"),
        DENIED("denied"),
        FAILURE("failure");

        private final String value;

        Result(String value) {
            this.value = value;
        }

        public String value() {
            return value;
        }
    }

    /**
     * Raised before a protected operation when its durable audit attempt cannot be committed.
     * The fixed message intentionally excludes the underlying database failure.
     */
    public static final class AuditUnavailableException extends RuntimeException {

        public AuditUnavailableException() {
            super("Administrator audit trail is unavailable");
        }
    }

    /**
     * Opaque correlation handle for one committed audit attempt. References are keyed HMACs and do
     * not contain raw usernames or target labels.
     */
    public record Attempt(Long id,
                          Long actorId,
                          String actorRef,
                          Long targetId,
                          String targetRef,
                          String action) {
    }

    /**
     * Wrap a controller mutation. A durable attempt is committed before invoking the operation. If
     * the controller participates in a transaction, finalization is deferred until that transaction
     * completes so a later rollback cannot leave a false success outcome.
     */
    public <T> ResponseEntity<T> capture(String targetType,
                                         Long targetId,
                                         String action,
                                         Supplier<ResponseEntity<T>> operation) {
        Objects.requireNonNull(operation, "operation");
        Attempt attempt = begin(targetType, targetId, action);
        try {
            ResponseEntity<T> response = operation.get();
            Outcome outcome = Outcome.fromStatus(response == null ? 500 : response.getStatusCode().value());
            scheduleCompletion(attempt, outcome, null, null);
            return response;
        } catch (RuntimeException exception) {
            scheduleCompletion(attempt, Outcome.fromException(exception), null, null);
            throw exception;
        }
    }

    /**
     * Wrap a mutation that creates one entity. The durable attempt is committed against the
     * code-owned {@code type:new} target before the operation starts. Only a successful 2xx
     * response is inspected by the code-owned extractor; its identifier and the successful
     * outcome are then persisted atomically. Extraction or outcome persistence failures leave the
     * original attempt intact and never replace the business response.
     */
    public <T> ResponseEntity<T> captureCreated(String targetType,
                                                String action,
                                                Supplier<ResponseEntity<T>> operation,
                                                Function<? super T, Long> targetIdExtractor) {
        Objects.requireNonNull(operation, "operation");
        Objects.requireNonNull(targetIdExtractor, "targetIdExtractor");
        Attempt attempt = begin(targetType, null, action);
        try {
            ResponseEntity<T> response = operation.get();
            Outcome outcome = Outcome.fromStatus(response == null ? 500 : response.getStatusCode().value());
            if (outcome.result() != Result.SUCCESS) {
                scheduleCompletion(attempt, outcome, null, null);
                return response;
            }

            try {
                Long createdTargetId = targetIdExtractor.apply(response.getBody());
                if (createdTargetId == null || createdTargetId <= 0) {
                    throw new IllegalStateException("Created audit target identifier is unavailable");
                }
                scheduleCompletion(
                        attempt,
                        outcome,
                        null,
                        null,
                        target(targetType, createdTargetId));
            } catch (RuntimeException exception) {
                logOutcomeFailure(attempt, outcome, exception);
            }
            return response;
        } catch (RuntimeException exception) {
            scheduleCompletion(attempt, Outcome.fromException(exception), null, null);
            throw exception;
        }
    }

    /**
     * Audit a mixed user/administrator endpoint only when the authenticated caller currently has
     * the administrator authority. Ordinary user operations bypass the administrator audit trail.
     */
    public <T> ResponseEntity<T> captureIfAdmin(String targetType,
                                                Long targetId,
                                                String action,
                                                Supplier<ResponseEntity<T>> operation) {
        Objects.requireNonNull(operation, "operation");
        if (!isCurrentAdministrator()) {
            return operation.get();
        }
        return capture(targetType, targetId, action, operation);
    }

    /** Commit an administrator attempt before a mutation is invoked. */
    public Attempt begin(String targetType, Long targetId, String action) {
        return begin(currentActor(), target(targetType, targetId), action);
    }

    /**
     * Commit an attempt for service methods that receive Authentication explicitly instead of
     * reading it from the security context.
     */
    public Attempt beginForActor(String actorUsername,
                                 String targetType,
                                 Long targetId,
                                 String action) {
        return begin(actor(actorUsername), target(targetType, targetId), action);
    }

    /** Finalize an attempt with code-owned labels. Failure leaves the committed attempt intact. */
    public void complete(Attempt attempt, Result result, String reason) {
        complete(attempt, result, reason, null, null);
    }

    /**
     * Finalize an attempt including bounded before/after role labels. Failure leaves the committed
     * attempt intact.
     */
    public void complete(Attempt attempt,
                         Result result,
                         String reason,
                         String beforeRole,
                         String afterRole) {
        Objects.requireNonNull(attempt, "attempt");
        Objects.requireNonNull(result, "result");
        scheduleCompletion(
                attempt,
                new Outcome(result, normalizeLabel(reason, "unspecified")),
                normalizeOptionalLabel(beforeRole, MAX_ROLE_LENGTH),
                normalizeOptionalLabel(afterRole, MAX_ROLE_LENGTH));
    }

    private Attempt begin(AuditActor actor, AuditTarget target, String action) {
        String normalizedAction = normalizeLabel(action, "admin_mutation");
        try {
            AdminAuditLog saved = requiresNewTransaction.execute(ignored ->
                    auditLogRepository.saveAndFlush(AdminAuditLog.create(
                            actor.id(),
                            actor.ref(),
                            target.id(),
                            target.ref(),
                            normalizedAction,
                            ATTEMPT_RESULT,
                            ATTEMPT_REASON,
                            null,
                            null)));
            if (saved == null || saved.getId() == null) {
                throw new IllegalStateException("Audit insert completed without an identifier");
            }
            return new Attempt(
                    saved.getId(),
                    actor.id(),
                    actor.ref(),
                    target.id(),
                    target.ref(),
                    normalizedAction);
        } catch (RuntimeException exception) {
            logger.error("Administrator audit preflight failed: action={}, targetRef={}, error={}",
                    normalizedAction,
                    target.ref(),
                    SensitiveLogSanitizer.exceptionSummary(exception));
            throw new AuditUnavailableException();
        }
    }

    private void scheduleCompletion(Attempt attempt,
                                    Outcome outcome,
                                    String beforeRole,
                                    String afterRole) {
        scheduleCompletion(attempt, outcome, beforeRole, afterRole, null);
    }

    private void scheduleCompletion(Attempt attempt,
                                    Outcome outcome,
                                    String beforeRole,
                                    String afterRole,
                                    AuditTarget successfulTarget) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            try {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        Outcome completed = transactionOutcome(status, outcome);
                        persistOutcome(attempt, completed, beforeRole, afterRole, successfulTarget);
                    }
                });
            } catch (RuntimeException exception) {
                logOutcomeFailure(attempt, outcome, exception);
            }
            return;
        }
        persistOutcome(attempt, outcome, beforeRole, afterRole, successfulTarget);
    }

    private Outcome transactionOutcome(int status, Outcome outcome) {
        if (outcome.result() != Result.SUCCESS || status == TransactionSynchronization.STATUS_COMMITTED) {
            return outcome;
        }
        return status == TransactionSynchronization.STATUS_ROLLED_BACK
                ? new Outcome(Result.FAILURE, "transaction_rollback")
                : new Outcome(Result.FAILURE, "transaction_unknown");
    }

    private void persistOutcome(Attempt attempt,
                                Outcome outcome,
                                String beforeRole,
                                String afterRole,
                                AuditTarget successfulTarget) {
        try {
            requiresNewTransaction.executeWithoutResult(ignored -> {
                AdminAuditLog log = auditLogRepository.findById(attempt.id())
                        .orElseThrow(() -> new IllegalStateException("Audit attempt is missing"));
                if (outcome.result() == Result.SUCCESS && successfulTarget != null) {
                    log.setTargetId(successfulTarget.id());
                    log.setTargetRef(successfulTarget.ref());
                }
                log.setResult(outcome.result().value());
                log.setReason(normalizeLabel(outcome.reason(), "unspecified"));
                log.setBeforeRole(beforeRole);
                log.setAfterRole(afterRole);
                auditLogRepository.flush();
            });
        } catch (RuntimeException exception) {
            // The committed attempt is deliberately not deleted or replaced. It is the durable
            // evidence that the operation may have run even though finalization was unavailable.
            logOutcomeFailure(attempt, outcome, exception);
        }
    }

    private void logOutcomeFailure(Attempt attempt, Outcome outcome, RuntimeException exception) {
        logger.error("Administrator audit outcome persistence failed: attemptId={}, action={}, result={}, targetRef={}, error={}",
                    attempt.id(),
                    attempt.action(),
                    outcome.result().value(),
                    attempt.targetRef(),
                    SensitiveLogSanitizer.exceptionSummary(exception));
    }

    private AuditActor currentActor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !authentication.isAuthenticated()
                || authentication instanceof AnonymousAuthenticationToken
                || !StringUtils.hasText(authentication.getName())) {
            return new AuditActor(null, ANONYMOUS_ACTOR_REF);
        }

        return actor(authentication.getName());
    }

    private AuditActor actor(String actorUsername) {
        if (!StringUtils.hasText(actorUsername)) {
            return new AuditActor(null, ANONYMOUS_ACTOR_REF);
        }

        String username = actorUsername.trim();
        String actorRef = cacheKeyHasher.cacheKey("admin-audit-actor", username);
        Long actorId = null;
        try {
            actorId = userRepository.findByUsername(username).map(user -> user.getId()).orElse(null);
        } catch (RuntimeException exception) {
            logger.warn("Unable to resolve administrator id for audit actorRef={}", actorRef);
        }
        return new AuditActor(actorId, actorRef);
    }

    private boolean isCurrentAdministrator() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)
                && authentication.getAuthorities().stream()
                        .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private AuditTarget target(String targetType, Long targetId) {
        String safeType = normalizeLabel(targetType, "unknown_target");
        String key = safeType + ":" + (targetId == null ? "new" : targetId);
        return new AuditTarget(targetId, cacheKeyHasher.cacheKey("admin-audit-target", key));
    }

    private static String normalizeLabel(String value, String fallback) {
        String normalized = StringUtils.hasText(value) ? value.trim() : fallback;
        normalized = normalized.replaceAll("[^A-Za-z0-9_.-]", "_");
        if (normalized.length() <= MAX_LABEL_LENGTH) {
            return normalized;
        }
        return normalized.substring(0, MAX_LABEL_LENGTH);
    }

    private static String normalizeOptionalLabel(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = value.trim().replaceAll("[^A-Za-z0-9_.-]", "_");
        return normalized.length() <= maxLength ? normalized : normalized.substring(0, maxLength);
    }

    private record AuditActor(Long id, String ref) {
    }

    private record AuditTarget(Long id, String ref) {
    }

    private record Outcome(Result result, String reason) {

        private static Outcome fromStatus(int status) {
            Result result = status >= 200 && status < 300
                    ? Result.SUCCESS
                    : status == 401 || status == 403
                            ? Result.DENIED
                            : Result.FAILURE;
            return new Outcome(result, "http_" + status);
        }

        private static Outcome fromException(RuntimeException exception) {
            String simpleName = exception == null ? "Exception" : exception.getClass().getSimpleName();
            return new Outcome(Result.FAILURE, "exception_" + simpleName);
        }
    }
}
