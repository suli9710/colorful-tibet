package com.tibet.tourism.modules.admin.application;

import com.tibet.tourism.common.security.LoginAttemptService;
import com.tibet.tourism.common.security.PiiCryptoConverter;
import com.tibet.tourism.modules.auth.application.AdminMfaPolicy;
import com.tibet.tourism.common.security.antibot.infra.BehaviorLogRepository;
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
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
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
    private final AdminAuditLogService adminAuditLogService;
    private final BehaviorLogRepository behaviorLogRepository;
    private final AdminMfaPolicy adminMfaPolicy;
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
                             AdminAuditLogService adminAuditLogService,
                             BehaviorLogRepository behaviorLogRepository,
                             AdminMfaPolicy adminMfaPolicy) {
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
        this.adminAuditLogService = adminAuditLogService;
        this.behaviorLogRepository = behaviorLogRepository;
        this.adminMfaPolicy = adminMfaPolicy;
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
        AdminAuditLogService.Attempt attempt = adminAuditLogService.beginForActor(
                operatorUsername, "user", targetUserId, ROLE_UPDATE_ACTION);
        try {
            Optional<User> userOpt = userRepository.findById(targetUserId);
            if (userOpt.isEmpty()) {
                auditRoleUpdate(attempt, AdminAuditLogService.Result.FAILURE,
                        "target_not_found", null, null);
                return new RoleUpdateResult(false, 404, "用户不存在");
            }
            return updateRoleInternal(userOpt.get(), role, authentication, operatorUsername, attempt);
        } catch (RuntimeException exception) {
            auditUnexpectedFailure(attempt, exception, null, null);
            throw exception;
        }
    }

    @Transactional
    public RoleUpdateResult updateRole(User user, String role, Authentication authentication) {
        String operatorUsername = authenticatedUsername(authentication);
        Long targetId = user == null ? null : user.getId();
        AdminAuditLogService.Attempt attempt = adminAuditLogService.beginForActor(
                operatorUsername, "user", targetId, ROLE_UPDATE_ACTION);
        try {
            return updateRoleInternal(user, role, authentication, operatorUsername, attempt);
        } catch (RuntimeException exception) {
            auditUnexpectedFailure(attempt, exception, roleName(user), roleName(user));
            throw exception;
        }
    }

    private RoleUpdateResult updateRoleInternal(User user,
                                                String role,
                                                Authentication authentication,
                                                String operatorUsername,
                                                AdminAuditLogService.Attempt attempt) {
        String beforeRole = roleName(user);

        if (user == null) {
            auditRoleUpdate(attempt, AdminAuditLogService.Result.FAILURE,
                    "target_not_found", null, null);
            return new RoleUpdateResult(false, 404, "用户不存在");
        }

        if (authentication == null) {
            auditRoleUpdate(attempt, AdminAuditLogService.Result.FAILURE,
                    "unauthenticated", beforeRole, beforeRole);
            return new RoleUpdateResult(false, 401, "未登录，不能修改用户角色");
        }
        if (role == null) {
            auditRoleUpdate(attempt, AdminAuditLogService.Result.FAILURE,
                    "role_required", beforeRole, beforeRole);
            return new RoleUpdateResult(false, 400, "角色不能为空");
        }

        User.Role requestedRole;
        try {
            requestedRole = User.Role.valueOf(role.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            auditRoleUpdate(attempt, AdminAuditLogService.Result.FAILURE,
                    "invalid_role", beforeRole, beforeRole);
            return new RoleUpdateResult(false, 400, "无效的角色");
        }

        boolean operatorIsSuperAdmin = isSuperAdminUsername(operatorUsername);
        String afterRole = requestedRole.name();

        if (isSuperAdminAccount(user)) {
            auditRoleUpdate(attempt, AdminAuditLogService.Result.DENIED,
                    "protected_account", beforeRole, afterRole);
            return new RoleUpdateResult(false, 400, "不能修改超级管理员角色");
        }
        if (sameUsername(operatorUsername, user.getUsername()) && requestedRole != User.Role.ADMIN) {
            auditRoleUpdate(attempt, AdminAuditLogService.Result.DENIED,
                    "self_admin_demotion", beforeRole, afterRole);
            return new RoleUpdateResult(false, 400, "不能取消自己的管理员权限");
        }
        if (!operatorIsSuperAdmin) {
            auditRoleUpdate(attempt, AdminAuditLogService.Result.DENIED,
                    "requires_super_admin", beforeRole, afterRole);
            return new RoleUpdateResult(false, 403, "只有超级管理员可以调整管理员角色");
        }

        if (requestedRole == User.Role.ADMIN
                && !adminMfaPolicy.isSuperAdmin(user)
                && !adminMfaPolicy.hasConfiguredSecret(user.getUsername())) {
            auditRoleUpdate(attempt, AdminAuditLogService.Result.DENIED,
                    "admin_mfa_not_configured", beforeRole, afterRole);
            return new RoleUpdateResult(false, 409,
                    "请先为该管理员配置独立的双因素认证密钥");
        }

        if (user.getRole() != requestedRole) {
            user.setRole(requestedRole);
            user.incrementSessionVersion();
        }
        userRepository.save(user);
        auditRoleUpdate(attempt, AdminAuditLogService.Result.SUCCESS,
                "authorized", beforeRole, afterRole);
        return new RoleUpdateResult(true, 200, "角色更新成功");
    }

    @Transactional
    public UnlockUserResult unlockLogin(Long targetUserId, Authentication authentication) {
        String operatorUsername = authenticatedUsername(authentication);
        AdminAuditLogService.Attempt attempt = adminAuditLogService.beginForActor(
                operatorUsername, "user", targetUserId, UNLOCK_LOGIN_ACTION);
        try {
            if (authentication == null) {
                auditUnlock(attempt, AdminAuditLogService.Result.FAILURE, "unauthenticated");
                return new UnlockUserResult(false, 401, "未登录，不能解除登录锁定");
            }

            Optional<User> targetUserOpt = userRepository.findById(targetUserId);
            if (targetUserOpt.isEmpty()) {
                auditUnlock(attempt, AdminAuditLogService.Result.FAILURE, "target_not_found");
                return new UnlockUserResult(false, 404, "用户不存在");
            }

            User targetUser = targetUserOpt.get();
            boolean operatorIsSuperAdmin = isSuperAdminUsername(operatorUsername);
            if (requiresSuperAdminToUnlock(targetUser) && !operatorIsSuperAdmin) {
                auditUnlock(attempt, AdminAuditLogService.Result.DENIED, "requires_super_admin");
                return new UnlockUserResult(false, 403, "只有超级管理员可以解除管理员账号锁定");
            }

            // This mutates Redis and the local fallback. The durable attempt above must therefore
            // commit before reset is invoked; finalization may fail without erasing that evidence.
            loginAttemptService.reset(targetUser.getUsername());
            auditUnlock(attempt, AdminAuditLogService.Result.SUCCESS, "authorized");
            return new UnlockUserResult(true, 200, "已解除登录锁定");
        } catch (RuntimeException exception) {
            auditUnexpectedFailure(attempt, exception, null, null);
            throw exception;
        }
    }

    @Transactional
    public DeleteUserResult deleteUser(Long targetUserId, Authentication authentication) {
        String operatorUsername = authenticatedUsername(authentication);
        AdminAuditLogService.Attempt attempt = adminAuditLogService.beginForActor(
                operatorUsername, "user", targetUserId, DELETE_USER_ACTION);
        try {
            Optional<User> userOpt = userRepository.findById(targetUserId);
            if (userOpt.isEmpty()) {
                auditDelete(attempt, AdminAuditLogService.Result.FAILURE, "target_not_found", null);
                return new DeleteUserResult(false, 404, "用户不存在");
            }
            return deleteUserInternal(userOpt.get(), authentication, operatorUsername, attempt);
        } catch (RuntimeException exception) {
            auditUnexpectedFailure(attempt, exception, null, null);
            throw exception;
        }
    }

    @Transactional
    public DeleteUserResult deleteUser(User targetUser, Authentication authentication) {
        String operatorUsername = authenticatedUsername(authentication);
        Long targetId = targetUser == null ? null : targetUser.getId();
        AdminAuditLogService.Attempt attempt = adminAuditLogService.beginForActor(
                operatorUsername, "user", targetId, DELETE_USER_ACTION);
        try {
            return deleteUserInternal(targetUser, authentication, operatorUsername, attempt);
        } catch (RuntimeException exception) {
            auditUnexpectedFailure(attempt, exception, roleName(targetUser), null);
            throw exception;
        }
    }

    private DeleteUserResult deleteUserInternal(User targetUser,
                                                Authentication authentication,
                                                String operatorUsername,
                                                AdminAuditLogService.Attempt attempt) {
        String beforeRole = roleName(targetUser);

        if (targetUser == null) {
            auditDelete(attempt, AdminAuditLogService.Result.FAILURE, "target_not_found", null);
            return new DeleteUserResult(false, 404, "用户不存在");
        }

        if (authentication == null) {
            auditDelete(attempt, AdminAuditLogService.Result.FAILURE, "unauthenticated", beforeRole);
            return new DeleteUserResult(false, 401, "未登录，不能删除用户");
        }
        if (!isSuperAdminUsername(operatorUsername)) {
            auditDelete(attempt, AdminAuditLogService.Result.DENIED, "requires_super_admin", beforeRole);
            return new DeleteUserResult(false, 403, "只有超级管理员可以删除用户");
        }
        if (isSuperAdminAccount(targetUser)) {
            auditDelete(attempt, AdminAuditLogService.Result.DENIED, "protected_account", beforeRole);
            return new DeleteUserResult(false, 400, "不能删除超级管理员账号");
        }
        if (isReservedSystemAccount(targetUser)) {
            // "official" 是官方内容（OFFICIAL 共享路线等）的系统作者账号，删除会级联清除全部官方内容。
            auditDelete(attempt, AdminAuditLogService.Result.DENIED, "protected_account", beforeRole);
            return new DeleteUserResult(false, 400, "不能删除系统账号");
        }
        if (sameUsername(operatorUsername, targetUser.getUsername())) {
            auditDelete(attempt, AdminAuditLogService.Result.DENIED, "self_delete", beforeRole);
            return new DeleteUserResult(false, 400, "不能删除当前登录账号");
        }
        if (targetUser.getRole() != User.Role.USER) {
            auditDelete(attempt, AdminAuditLogService.Result.DENIED, "target_not_user", beforeRole);
            return new DeleteUserResult(false, 403, "只能删除普通用户账号");
        }

        Long userId = targetUser.getId();

        // Capture the content owned by OTHER users that this account engaged with. Deleting the rows
        // below without recomputing their denormalised counters leaves those comments, routes and
        // questions permanently reporting more likes/comments/answers than actually exist.
        List<Long> likedCommentIds = commentLikeRepository.findLikedCommentIdsByUserId(userId);
        List<Long> likedRouteIds = routeLikeRepository.findLikedRouteIdsByUserId(userId);
        List<Long> commentedRouteIds = routeCommentRepository.findCommentedRouteIdsByUserId(userId);
        List<Long> likedQuestionIds = questionLikeRepository.findLikedQuestionIdsByUserId(userId);
        List<Long> answeredQuestionIds = travelAnswerRepository.findAnsweredQuestionIdsByUserId(userId);

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
        behaviorLogRepository.deleteByUserId(userId);

        // Recount rather than decrement: recomputing from the surviving rows is idempotent and also
        // repairs counters that drifted before this fix existed.
        recountIfAny(likedCommentIds, commentRepository::recountLikes);
        recountIfAny(likedRouteIds, sharedRouteRepository::recountLikes);
        recountIfAny(commentedRouteIds, sharedRouteRepository::recountComments);
        recountIfAny(likedQuestionIds, travelQuestionRepository::recountLikes);
        recountIfAny(answeredQuestionIds, travelQuestionRepository::recountAnswers);

        userRepository.delete(targetUser);
        auditDelete(attempt, AdminAuditLogService.Result.SUCCESS, "authorized", beforeRole);
        return new DeleteUserResult(true, 200, "用户删除成功");
    }

    private static void recountIfAny(List<Long> ids, Consumer<Collection<Long>> recount) {
        if (ids != null && !ids.isEmpty()) {
            recount.accept(ids);
        }
    }

    private String authenticatedUsername(Authentication authentication) {
        return authentication == null || authentication.getName() == null ? "" : authentication.getName();
    }

    private boolean isSuperAdminAccount(User user) {
        return user != null && isSuperAdminUsername(user.getUsername());
    }

    private static final String RESERVED_OFFICIAL_USERNAME = "official";

    private boolean isReservedSystemAccount(User user) {
        return user != null
                && StringUtils.hasText(user.getUsername())
                && RESERVED_OFFICIAL_USERNAME.equalsIgnoreCase(user.getUsername().trim());
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

    private String roleName(User user) {
        return user == null || user.getRole() == null ? null : user.getRole().name();
    }

    private void auditUnlock(AdminAuditLogService.Attempt attempt,
                             AdminAuditLogService.Result result,
                             String reason) {
        adminAuditLogService.complete(attempt, result, reason);
        securityAuditLogger.info(
                "action={} result={} reason={} actorId={} actorRef={} targetId={} targetRef={}",
                UNLOCK_LOGIN_ACTION,
                result.value(),
                reason,
                attempt.actorId(),
                attempt.actorRef(),
                attempt.targetId(),
                attempt.targetRef());
    }

    private void auditRoleUpdate(AdminAuditLogService.Attempt attempt,
                                 AdminAuditLogService.Result result,
                                 String reason,
                                 String beforeRole,
                                 String afterRole) {
        adminAuditLogService.complete(attempt, result, reason, beforeRole, afterRole);
    }

    private void auditDelete(AdminAuditLogService.Attempt attempt,
                             AdminAuditLogService.Result result,
                             String reason,
                             String beforeRole) {
        adminAuditLogService.complete(attempt, result, reason, beforeRole, null);
    }

    private void auditUnexpectedFailure(AdminAuditLogService.Attempt attempt,
                                        RuntimeException exception,
                                        String beforeRole,
                                        String afterRole) {
        String type = exception == null ? "Exception" : exception.getClass().getSimpleName();
        adminAuditLogService.complete(
                attempt,
                AdminAuditLogService.Result.FAILURE,
                "exception_" + type,
                beforeRole,
                afterRole);
    }

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
