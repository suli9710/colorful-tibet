package com.tibet.tourism.modules.admin.application;

import com.tibet.tourism.common.security.LoginAttemptService;
import com.tibet.tourism.common.security.PiiCryptoConverter;
import com.tibet.tourism.common.security.PiiMasker;
import com.tibet.tourism.modules.admin.domain.AdminAuditLog;
import com.tibet.tourism.modules.admin.infra.AdminAuditLogRepository;
import com.tibet.tourism.modules.ai.infra.AiRouteRecordRepository;
import com.tibet.tourism.modules.community.infra.CommentLikeRepository;
import com.tibet.tourism.modules.community.infra.CommentRepository;
import com.tibet.tourism.modules.community.infra.FavoriteRepository;
import com.tibet.tourism.modules.community.infra.QuestionLikeRepository;
import com.tibet.tourism.modules.community.infra.RouteCommentRepository;
import com.tibet.tourism.modules.community.infra.RouteLikeRepository;
import com.tibet.tourism.modules.community.infra.SharedRouteRepository;
import com.tibet.tourism.modules.community.infra.TravelAnswerRepository;
import com.tibet.tourism.modules.community.infra.TravelQuestionRepository;
import com.tibet.tourism.modules.hotel.infra.HotelBookingRepository;
import com.tibet.tourism.modules.order.infra.BookingRepository;
import com.tibet.tourism.modules.order.infra.OrderAuditLogRepository;
import com.tibet.tourism.modules.order.infra.PlatformOrderRepository;
import com.tibet.tourism.modules.route.infra.ItineraryRepository;
import com.tibet.tourism.modules.route.infra.TibetTravelKitRepository;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import com.tibet.tourism.modules.user.infra.UserVisitHistoryRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.util.Locale;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminUserService {

    private static final Logger securityAuditLogger = LoggerFactory.getLogger("security.audit.admin-user");
    private static final String ANONYMIZED_LABEL = "Deleted user";
    private static final String UNLOCK_LOGIN_ACTION = "admin_user_unlock_login";
    private static final String ROLE_UPDATE_ACTION = "admin_user_role_update";
    private static final String DELETE_USER_ACTION = "admin_user_delete";
    private static final String RESULT_SUCCESS = "success";
    private static final String RESULT_DENIED = "denied";
    private static final String RESULT_FAILURE = "failure";

    private final UserRepository userRepository;
    private final LoginAttemptService loginAttemptService;
    private final CommentLikeRepository commentLikeRepository;
    private final RouteLikeRepository routeLikeRepository;
    private final RouteCommentRepository routeCommentRepository;
    private final CommentRepository commentRepository;
    private final SharedRouteRepository sharedRouteRepository;
    private final FavoriteRepository favoriteRepository;
    private final QuestionLikeRepository questionLikeRepository;
    private final TravelAnswerRepository travelAnswerRepository;
    private final TravelQuestionRepository travelQuestionRepository;
    private final BookingRepository bookingRepository;
    private final PlatformOrderRepository platformOrderRepository;
    private final OrderAuditLogRepository orderAuditLogRepository;
    private final HotelBookingRepository hotelBookingRepository;
    private final AiRouteRecordRepository aiRouteRecordRepository;
    private final TibetTravelKitRepository tibetTravelKitRepository;
    private final ItineraryRepository itineraryRepository;
    private final UserVisitHistoryRepository userVisitHistoryRepository;
    private final AdminAuditLogRepository adminAuditLogRepository;
    private final PiiCryptoConverter piiCryptoConverter = new PiiCryptoConverter();

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.super-admin-username:}")
    private String superAdminUsername;

    public AdminUserService(UserRepository userRepository,
                            LoginAttemptService loginAttemptService,
                            CommentLikeRepository commentLikeRepository,
                            RouteLikeRepository routeLikeRepository,
                            RouteCommentRepository routeCommentRepository,
                            CommentRepository commentRepository,
                            SharedRouteRepository sharedRouteRepository,
                            FavoriteRepository favoriteRepository,
                            QuestionLikeRepository questionLikeRepository,
                            TravelAnswerRepository travelAnswerRepository,
                            TravelQuestionRepository travelQuestionRepository,
                            BookingRepository bookingRepository,
                            PlatformOrderRepository platformOrderRepository,
                            OrderAuditLogRepository orderAuditLogRepository,
                            HotelBookingRepository hotelBookingRepository,
                            AiRouteRecordRepository aiRouteRecordRepository,
                            TibetTravelKitRepository tibetTravelKitRepository,
                            ItineraryRepository itineraryRepository,
                            UserVisitHistoryRepository userVisitHistoryRepository,
                            AdminAuditLogRepository adminAuditLogRepository) {
        this.userRepository = userRepository;
        this.loginAttemptService = loginAttemptService;
        this.commentLikeRepository = commentLikeRepository;
        this.routeLikeRepository = routeLikeRepository;
        this.routeCommentRepository = routeCommentRepository;
        this.commentRepository = commentRepository;
        this.sharedRouteRepository = sharedRouteRepository;
        this.favoriteRepository = favoriteRepository;
        this.questionLikeRepository = questionLikeRepository;
        this.travelAnswerRepository = travelAnswerRepository;
        this.travelQuestionRepository = travelQuestionRepository;
        this.bookingRepository = bookingRepository;
        this.platformOrderRepository = platformOrderRepository;
        this.orderAuditLogRepository = orderAuditLogRepository;
        this.hotelBookingRepository = hotelBookingRepository;
        this.aiRouteRecordRepository = aiRouteRecordRepository;
        this.tibetTravelKitRepository = tibetTravelKitRepository;
        this.itineraryRepository = itineraryRepository;
        this.userVisitHistoryRepository = userVisitHistoryRepository;
        this.adminAuditLogRepository = adminAuditLogRepository;
    }

    public record RoleUpdateResult(boolean success, int status, String message) {}
    public record DeleteUserResult(boolean success, int status, String message) {}
    public record UnlockUserResult(boolean success, int status, String message) {}
    public record UserActionPolicy(boolean protectedAccount, boolean deletable, boolean roleMutable) {}

    public UserActionPolicy actionPolicyFor(User targetUser, Authentication authentication) {
        String operatorUsername = authenticatedUsername(authentication);
        String targetUsername = targetUser == null ? "" : targetUser.getUsername();
        User.Role targetRole = targetUser == null ? null : targetUser.getRole();
        boolean protectedAccount = isSuperAdminAccount(targetUser);
        boolean operatorIsSuperAdmin = isSuperAdminUsername(operatorUsername);
        boolean operatorIsTarget = sameUsername(operatorUsername, targetUsername);

        boolean deletable = operatorIsSuperAdmin
                && !protectedAccount
                && !operatorIsTarget
                && targetRole == User.Role.USER;
        boolean roleMutable = operatorIsSuperAdmin
                && !protectedAccount
                && !operatorIsTarget;

        return new UserActionPolicy(protectedAccount, deletable, roleMutable);
    }

    @Transactional
    public RoleUpdateResult updateRole(Long targetUserId, String role, Authentication authentication) {
        String operatorUsername = authenticatedUsername(authentication);
        AuditActor actor = auditActor(operatorUsername);
        Optional<User> userOpt = userRepository.findById(targetUserId);
        if (userOpt.isEmpty()) {
            auditRoleUpdate(actor, targetUserId, "user#missing", RESULT_FAILURE,
                    "target_not_found", null, null);
            return new RoleUpdateResult(false, 404, "用户不存在");
        }
        return updateRole(userOpt.get(), role, authentication);
    }

    @Transactional
    public RoleUpdateResult updateRole(User user, String role, Authentication authentication) {
        String operatorUsername = authenticatedUsername(authentication);
        AuditActor actor = auditActor(operatorUsername);
        Long targetId = user == null ? null : user.getId();
        String targetRef = auditRef(user);
        String beforeRole = roleName(user);

        if (authentication == null) {
            auditRoleUpdate(actor, targetId, targetRef, RESULT_FAILURE, "unauthenticated", beforeRole, beforeRole);
            return new RoleUpdateResult(false, 401, "未登录，不能修改用户角色");
        }
        if (role == null) {
            auditRoleUpdate(actor, targetId, targetRef, RESULT_FAILURE, "role_required", beforeRole, beforeRole);
            return new RoleUpdateResult(false, 400, "角色不能为空");
        }
        try {
            User.Role requestedRole = User.Role.valueOf(role.trim().toUpperCase(Locale.ROOT));
            boolean operatorIsSuperAdmin = isSuperAdminUsername(operatorUsername);
            String afterRole = requestedRole.name();

            if (isSuperAdminAccount(user)) {
                auditRoleUpdate(actor, targetId, targetRef, RESULT_DENIED, "protected_account", beforeRole, afterRole);
                return new RoleUpdateResult(false, 400, "不能修改超级管理员角色");
            }
            if (sameUsername(operatorUsername, user.getUsername()) && requestedRole != User.Role.ADMIN) {
                auditRoleUpdate(actor, targetId, targetRef, RESULT_DENIED, "self_admin_demotion", beforeRole, afterRole);
                return new RoleUpdateResult(false, 400, "不能取消自己的管理员权限");
            }
            if (!operatorIsSuperAdmin) {
                auditRoleUpdate(actor, targetId, targetRef, RESULT_DENIED, "requires_super_admin", beforeRole, afterRole);
                return new RoleUpdateResult(false, 403, "只有超级管理员可以调整管理员角色");
            }

            if (user.getRole() != requestedRole) {
                user.setRole(requestedRole);
                user.incrementSessionVersion();
            }
            userRepository.save(user);
            auditRoleUpdate(actor, targetId, targetRef, RESULT_SUCCESS, "authorized", beforeRole, afterRole);
            return new RoleUpdateResult(true, 200, "角色更新成功");
        } catch (IllegalArgumentException e) {
            auditRoleUpdate(actor, targetId, targetRef, RESULT_FAILURE, "invalid_role", beforeRole, beforeRole);
            return new RoleUpdateResult(false, 400, "无效的角色");
        }
    }

    @Transactional
    public UnlockUserResult unlockLogin(Long targetUserId, Authentication authentication) {
        String operatorUsername = authenticatedUsername(authentication);
        AuditActor actor = auditActor(operatorUsername);

        if (authentication == null) {
            auditUnlock(actor, targetUserId, "user#unknown", RESULT_FAILURE, "unauthenticated");
            return new UnlockUserResult(false, 401, "未登录，不能解除登录锁定");
        }

        Optional<User> targetUserOpt = userRepository.findById(targetUserId);
        if (targetUserOpt.isEmpty()) {
            auditUnlock(actor, targetUserId, "user#missing", RESULT_FAILURE, "target_not_found");
            return new UnlockUserResult(false, 404, "用户不存在");
        }

        User targetUser = targetUserOpt.get();
        String targetRef = auditRef(targetUser);
        boolean operatorIsSuperAdmin = isSuperAdminUsername(operatorUsername);
        if (requiresSuperAdminToUnlock(targetUser) && !operatorIsSuperAdmin) {
            auditUnlock(actor, targetUser.getId(), targetRef, RESULT_DENIED, "requires_super_admin");
            return new UnlockUserResult(false, 403, "只有超级管理员可以解除管理员账号锁定");
        }

        loginAttemptService.reset(targetUser.getUsername());
        auditUnlock(actor, targetUser.getId(), targetRef, RESULT_SUCCESS, "authorized");
        return new UnlockUserResult(true, 200, "已解除登录锁定");
    }

    @Transactional
    public DeleteUserResult deleteUser(Long targetUserId, Authentication authentication) {
        String operatorUsername = authenticatedUsername(authentication);
        AuditActor actor = auditActor(operatorUsername);
        Optional<User> userOpt = userRepository.findById(targetUserId);
        if (userOpt.isEmpty()) {
            auditDelete(actor, targetUserId, "user#missing", RESULT_FAILURE,
                    "target_not_found", null);
            return new DeleteUserResult(false, 404, "用户不存在");
        }
        return deleteUser(userOpt.get(), authentication);
    }

    @Transactional
    public DeleteUserResult deleteUser(User targetUser, Authentication authentication) {
        String operatorUsername = authenticatedUsername(authentication);
        AuditActor actor = auditActor(operatorUsername);
        Long targetId = targetUser == null ? null : targetUser.getId();
        String targetRef = auditRef(targetUser);
        String beforeRole = roleName(targetUser);

        if (authentication == null) {
            auditDelete(actor, targetId, targetRef, RESULT_FAILURE, "unauthenticated", beforeRole);
            return new DeleteUserResult(false, 401, "未登录，不能删除用户");
        }
        if (!isSuperAdminUsername(operatorUsername)) {
            auditDelete(actor, targetId, targetRef, RESULT_DENIED, "requires_super_admin", beforeRole);
            return new DeleteUserResult(false, 403, "只有超级管理员可以删除用户");
        }
        if (isSuperAdminAccount(targetUser)) {
            auditDelete(actor, targetId, targetRef, RESULT_DENIED, "protected_account", beforeRole);
            return new DeleteUserResult(false, 400, "不能删除超级管理员账号");
        }
        if (sameUsername(operatorUsername, targetUser.getUsername())) {
            auditDelete(actor, targetId, targetRef, RESULT_DENIED, "self_delete", beforeRole);
            return new DeleteUserResult(false, 400, "不能删除当前登录账号");
        }
        if (targetUser.getRole() != User.Role.USER) {
            auditDelete(actor, targetId, targetRef, RESULT_DENIED, "target_not_user", beforeRole);
            return new DeleteUserResult(false, 403, "只能删除普通用户账号");
        }

        Long userId = targetUser.getId();
        commentLikeRepository.deleteByCommentUserId(userId);
        commentLikeRepository.deleteByUserId(userId);
        routeLikeRepository.deleteByRouteAuthorId(userId);
        routeCommentRepository.deleteByRouteAuthorId(userId);
        routeLikeRepository.deleteByUser(targetUser);
        routeCommentRepository.deleteByUser(targetUser);
        favoriteRepository.deleteByUser(targetUser);
        questionLikeRepository.deleteByQuestionAuthorId(userId);
        questionLikeRepository.deleteByUser(targetUser);
        travelAnswerRepository.deleteByQuestionAuthorId(userId);
        travelAnswerRepository.deleteByUser(targetUser);
        travelQuestionRepository.deleteByAuthor(targetUser);
        commentRepository.deleteByUser(targetUser);
        sharedRouteRepository.deleteByAuthor(targetUser);
        aiRouteRecordRepository.deleteByUserId(userId);
        orderAuditLogRepository.clearActorUserByUserId(userId);
        anonymizeFinancialRecords(userId);
        tibetTravelKitRepository.deleteByItineraryUserId(userId);
        tibetTravelKitRepository.deleteByUserId(userId);
        itineraryRepository.clearParentReferencesToUserItineraries(userId);
        itineraryRepository.deleteByUserId(userId);
        userVisitHistoryRepository.deleteByUserId(userId);
        userRepository.delete(targetUser);
        auditDelete(actor, targetId, targetRef, RESULT_SUCCESS, "authorized", beforeRole);
        return new DeleteUserResult(true, 200, "用户删除成功");
    }

    private String authenticatedUsername(Authentication authentication) {
        return authentication == null || authentication.getName() == null ? "" : authentication.getName();
    }

    private boolean isSuperAdminAccount(User user) {
        return user != null && isSuperAdminUsername(user.getUsername());
    }

    private boolean requiresSuperAdminToUnlock(User targetUser) {
        return targetUser != null
                && (targetUser.getRole() == User.Role.ADMIN || isSuperAdminAccount(targetUser));
    }

    private boolean isSuperAdminUsername(String username) {
        return StringUtils.hasText(superAdminUsername)
                && StringUtils.hasText(username)
                && superAdminUsername.trim().equalsIgnoreCase(username.trim());
    }

    private boolean sameUsername(String first, String second) {
        return StringUtils.hasText(first)
                && StringUtils.hasText(second)
                && first.trim().equalsIgnoreCase(second.trim());
    }

    private AuditActor auditActor(String username) {
        if (username == null || username.isBlank()) {
            return new AuditActor(null, "user#anonymous");
        }
        Optional<User> actorUser = userRepository.findByUsername(username);
        Long actorId = (actorUser == null ? Optional.<User>empty() : actorUser)
                .map(User::getId)
                .orElse(null);
        return new AuditActor(actorId, auditRef(username));
    }

    private String auditRef(User user) {
        if (user == null) {
            return "user#unknown";
        }
        return auditRef(user.getUsername());
    }

    private String auditRef(String username) {
        return "user#" + PiiMasker.shortHash(username);
    }

    private String roleName(User user) {
        return user == null || user.getRole() == null ? null : user.getRole().name();
    }

    private void auditUnlock(AuditActor actor,
                             Long targetId,
                             String targetRef,
                             String result,
                             String reason) {
        saveAudit(actor, targetId, targetRef, UNLOCK_LOGIN_ACTION, result, reason, null, null);
        securityAuditLogger.info(
                "action={} result={} reason={} actorId={} actorRef={} targetId={} targetRef={}",
                UNLOCK_LOGIN_ACTION,
                result,
                reason,
                actor.id(),
                actor.ref(),
                targetId,
                targetRef);
    }

    private void auditRoleUpdate(AuditActor actor,
                                 Long targetId,
                                 String targetRef,
                                 String result,
                                 String reason,
                                 String beforeRole,
                                 String afterRole) {
        saveAudit(actor, targetId, targetRef, ROLE_UPDATE_ACTION, result, reason, beforeRole, afterRole);
    }

    private void auditDelete(AuditActor actor,
                             Long targetId,
                             String targetRef,
                             String result,
                             String reason,
                             String beforeRole) {
        saveAudit(actor, targetId, targetRef, DELETE_USER_ACTION, result, reason, beforeRole, null);
    }

    private void saveAudit(AuditActor actor,
                           Long targetId,
                           String targetRef,
                           String action,
                           String result,
                           String reason,
                           String beforeRole,
                           String afterRole) {
        adminAuditLogRepository.save(AdminAuditLog.create(
                actor.id(),
                actor.ref(),
                targetId,
                targetRef,
                action,
                result,
                reason,
                beforeRole,
                afterRole));
    }

    private record AuditActor(Long id, String ref) {}

    private void anonymizeFinancialRecords(Long userId) {
        String encryptedAnonymousLabel = piiCryptoConverter.convertToDatabaseColumn(ANONYMIZED_LABEL);
        executeNativeUpdate("""
                UPDATE invoices i
                JOIN orders o ON i.order_id = o.id
                SET i.invoice_title = :anonymousLabel,
                    i.tax_no = NULL
                WHERE o.user_id = :userId
                """, userId, encryptedAnonymousLabel);
        executeNativeUpdate("""
                UPDATE orders
                SET user_id = NULL,
                    idempotency_key = NULL,
                    customer_name = :anonymousLabel,
                    customer_phone = NULL,
                    customer_note = NULL,
                    support_note = NULL
                WHERE user_id = :userId
                """, userId, encryptedAnonymousLabel);
        executeNativeUpdate("""
                UPDATE bookings
                SET user_id = NULL
                WHERE user_id = :userId
                """, userId, null);
        executeNativeUpdate("""
                UPDATE hotel_bookings
                SET user_id = NULL,
                    guest_name = :anonymousLabel,
                    phone = NULL,
                    note = NULL
                WHERE user_id = :userId
                """, userId, encryptedAnonymousLabel);
    }

    private void executeNativeUpdate(String sql, Long userId, String anonymousLabel) {
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        if (anonymousLabel != null) {
            query.setParameter("anonymousLabel", anonymousLabel);
        }
        query.executeUpdate();
    }
}
