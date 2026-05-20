package com.tibet.tourism.service.admin;

import com.tibet.tourism.entity.User;
import com.tibet.tourism.repository.BookingRepository;
import com.tibet.tourism.repository.CommentLikeRepository;
import com.tibet.tourism.repository.CommentRepository;
import com.tibet.tourism.repository.HotelBookingRepository;
import com.tibet.tourism.repository.RouteCommentRepository;
import com.tibet.tourism.repository.RouteLikeRepository;
import com.tibet.tourism.repository.SharedRouteRepository;
import com.tibet.tourism.repository.UserRepository;
import com.tibet.tourism.repository.UserVisitHistoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final RouteLikeRepository routeLikeRepository;
    private final RouteCommentRepository routeCommentRepository;
    private final CommentRepository commentRepository;
    private final SharedRouteRepository sharedRouteRepository;
    private final BookingRepository bookingRepository;
    private final HotelBookingRepository hotelBookingRepository;
    private final UserVisitHistoryRepository userVisitHistoryRepository;

    @Value("${app.super-admin-username:lzh}")
    private String superAdminUsername;

    public AdminUserService(UserRepository userRepository,
                            CommentLikeRepository commentLikeRepository,
                            RouteLikeRepository routeLikeRepository,
                            RouteCommentRepository routeCommentRepository,
                            CommentRepository commentRepository,
                            SharedRouteRepository sharedRouteRepository,
                            BookingRepository bookingRepository,
                            HotelBookingRepository hotelBookingRepository,
                            UserVisitHistoryRepository userVisitHistoryRepository) {
        this.userRepository = userRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.routeLikeRepository = routeLikeRepository;
        this.routeCommentRepository = routeCommentRepository;
        this.commentRepository = commentRepository;
        this.sharedRouteRepository = sharedRouteRepository;
        this.bookingRepository = bookingRepository;
        this.hotelBookingRepository = hotelBookingRepository;
        this.userVisitHistoryRepository = userVisitHistoryRepository;
    }

    public record RoleUpdateResult(boolean success, int status, String message) {}

    public RoleUpdateResult updateRole(User user, String role, Authentication authentication) {
        if (role == null) {
            return new RoleUpdateResult(false, 400, "角色不能为空");
        }
        try {
            User.Role requestedRole = User.Role.valueOf(role.toUpperCase());
            String operatorUsername = authentication == null ? "" : authentication.getName();
            boolean operatorIsSuperAdmin = superAdminUsername.equals(operatorUsername);

            if (superAdminUsername.equals(user.getUsername())) {
                return new RoleUpdateResult(false, 400, "不能修改超级管理员角色");
            }
            if (operatorUsername.equals(user.getUsername()) && requestedRole != User.Role.ADMIN) {
                return new RoleUpdateResult(false, 400, "不能取消自己的管理员权限");
            }
            if (!operatorIsSuperAdmin && (requestedRole == User.Role.ADMIN || user.getRole() == User.Role.ADMIN)) {
                return new RoleUpdateResult(false, 403, "只有超级管理员可以调整管理员角色");
            }

            user.setRole(requestedRole);
            userRepository.save(user);
            return new RoleUpdateResult(true, 200, "角色更新成功");
        } catch (IllegalArgumentException e) {
            return new RoleUpdateResult(false, 400, "无效的角色");
        }
    }

    @Transactional
    public String deleteUser(User targetUser, Authentication authentication) {
        String operatorUsername = authentication == null ? "anonymous" : authentication.getName();

        if (authentication == null || !superAdminUsername.equals(operatorUsername)) {
            return "只有超级管理员可以删除用户账户";
        }

        if (superAdminUsername.equals(targetUser.getUsername())) {
            return "不能删除超级管理员账户";
        }

        Long userId = targetUser.getId();
        commentLikeRepository.deleteByUserId(userId);
        routeLikeRepository.deleteByUser(targetUser);
        routeCommentRepository.deleteByUser(targetUser);
        commentRepository.deleteByUser(targetUser);
        sharedRouteRepository.deleteByAuthor(targetUser);
        bookingRepository.deleteByUserId(userId);
        hotelBookingRepository.deleteByUserId(userId);
        userVisitHistoryRepository.deleteByUserId(userId);
        userRepository.delete(targetUser);
        return null;
    }
}
