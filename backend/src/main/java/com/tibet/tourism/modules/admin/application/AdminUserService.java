package com.tibet.tourism.modules.admin.application;

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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private static final String ANONYMIZED_LABEL = "Deleted user";

    private final UserRepository userRepository;
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

    @PersistenceContext
    private EntityManager entityManager;

    @Value("${app.super-admin-username:}")
    private String superAdminUsername;

    public AdminUserService(UserRepository userRepository,
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
                            UserVisitHistoryRepository userVisitHistoryRepository) {
        this.userRepository = userRepository;
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
    }

    public record RoleUpdateResult(boolean success, int status, String message) {}
    public record DeleteUserResult(boolean success, int status, String message) {}
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

    public RoleUpdateResult updateRole(User user, String role, Authentication authentication) {
        if (role == null) {
            return new RoleUpdateResult(false, 400, "角色不能为空");
        }
        try {
            User.Role requestedRole = User.Role.valueOf(role.toUpperCase());
            String operatorUsername = authenticatedUsername(authentication);
            boolean operatorIsSuperAdmin = isSuperAdminUsername(operatorUsername);

            if (isSuperAdminAccount(user)) {
                return new RoleUpdateResult(false, 400, "不能修改超级管理员角色");
            }
            if (sameUsername(operatorUsername, user.getUsername()) && requestedRole != User.Role.ADMIN) {
                return new RoleUpdateResult(false, 400, "不能取消自己的管理员权限");
            }
            if (!operatorIsSuperAdmin && (requestedRole == User.Role.ADMIN || user.getRole() == User.Role.ADMIN)) {
                return new RoleUpdateResult(false, 403, "只有超级管理员可以调整管理员角色");
            }

            if (user.getRole() != requestedRole) {
                user.setRole(requestedRole);
                user.incrementSessionVersion();
            }
            userRepository.save(user);
            return new RoleUpdateResult(true, 200, "角色更新成功");
        } catch (IllegalArgumentException e) {
            return new RoleUpdateResult(false, 400, "无效的角色");
        }
    }

    @Transactional
    public DeleteUserResult deleteUser(User targetUser, Authentication authentication) {
        String operatorUsername = authenticatedUsername(authentication);

        if (authentication == null) {
            return new DeleteUserResult(false, 401, "未登录，不能删除用户");
        }
        if (!isSuperAdminUsername(operatorUsername)) {
            return new DeleteUserResult(false, 403, "只有超级管理员可以删除用户");
        }
        if (isSuperAdminAccount(targetUser)) {
            return new DeleteUserResult(false, 400, "不能删除超级管理员账号");
        }
        if (sameUsername(operatorUsername, targetUser.getUsername())) {
            return new DeleteUserResult(false, 400, "不能删除当前登录账号");
        }
        if (targetUser.getRole() != User.Role.USER) {
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
        return new DeleteUserResult(true, 200, "用户删除成功");
    }

    private String authenticatedUsername(Authentication authentication) {
        return authentication == null || authentication.getName() == null ? "" : authentication.getName();
    }

    private boolean isSuperAdminAccount(User user) {
        return user != null && isSuperAdminUsername(user.getUsername());
    }

    private boolean isSuperAdminUsername(String username) {
        return superAdminUsername != null
                && !superAdminUsername.isBlank()
                && superAdminUsername.equals(username);
    }

    private boolean sameUsername(String first, String second) {
        return first != null && first.equals(second);
    }

    private void anonymizeFinancialRecords(Long userId) {
        executeNativeUpdate("""
                UPDATE invoices i
                JOIN orders o ON i.order_id = o.id
                SET i.invoice_title = :anonymousLabel,
                    i.tax_no = NULL
                WHERE o.user_id = :userId
                """, userId, true);
        executeNativeUpdate("""
                UPDATE orders
                SET user_id = NULL,
                    idempotency_key = NULL,
                    customer_name = :anonymousLabel,
                    customer_phone = NULL,
                    customer_note = NULL,
                    support_note = NULL
                WHERE user_id = :userId
                """, userId, true);
        executeNativeUpdate("""
                UPDATE bookings
                SET user_id = NULL
                WHERE user_id = :userId
                """, userId, false);
        executeNativeUpdate("""
                UPDATE hotel_bookings
                SET user_id = NULL,
                    guest_name = :anonymousLabel,
                    phone = NULL,
                    note = NULL
                WHERE user_id = :userId
                """, userId, true);
    }

    private void executeNativeUpdate(String sql, Long userId, boolean usesAnonymousLabel) {
        Query query = entityManager.createNativeQuery(sql);
        query.setParameter("userId", userId);
        if (usesAnonymousLabel) {
            query.setParameter("anonymousLabel", ANONYMIZED_LABEL);
        }
        query.executeUpdate();
    }
}
