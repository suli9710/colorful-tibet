package com.tibet.tourism.modules.user.application;
import com.tibet.tourism.common.error.BusinessException;
import com.tibet.tourism.common.error.ResourceNotFoundException;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.community.infra.CommentRepository;
import com.tibet.tourism.modules.community.infra.RouteCommentRepository;
import com.tibet.tourism.modules.community.infra.SharedRouteRepository;
import com.tibet.tourism.modules.order.infra.BookingRepository;
import com.tibet.tourism.modules.upload.application.FileStorageService;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
public class CurrentUserApplicationService {

    private final UserRepository userRepository;
    private final SharedRouteRepository sharedRouteRepository;
    private final RouteCommentRepository routeCommentRepository;
    private final BookingRepository bookingRepository;
    private final CommentRepository commentRepository;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    public CurrentUserApplicationService(
            UserRepository userRepository,
            SharedRouteRepository sharedRouteRepository,
            RouteCommentRepository routeCommentRepository,
            BookingRepository bookingRepository,
            CommentRepository commentRepository,
            UserService userService,
            FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.sharedRouteRepository = sharedRouteRepository;
        this.routeCommentRepository = routeCommentRepository;
        this.bookingRepository = bookingRepository;
        this.commentRepository = commentRepository;
        this.userService = userService;
        this.fileStorageService = fileStorageService;
    }

    public Map<String, Object> getProfile(Long userId) {
        User user = getUser(userId);

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("nickname", user.getNickname());
        response.put("avatar", user.getAvatar());
        response.put("phone", user.getPhone());
        response.put("role", user.getRole());
        response.put("createdAt", user.getCreatedAt());
        response.put("mustChangePassword", Boolean.TRUE.equals(user.getMustChangePassword()));
        return response;
    }

    public Map<String, Object> getStats(Long userId) {
        User user = getUser(userId);

        Map<String, Object> stats = new HashMap<>();
        stats.put("routeCount", sharedRouteRepository.findByAuthorOrderByCreatedAtDesc(user).size());
        stats.put("commentCount", routeCommentRepository.countByUser(user) + commentRepository.countByUser(user));
        stats.put("bookingCount", bookingRepository.findByUserId(userId).size());
        return stats;
    }

    public void changePassword(Long userId, String oldPassword, String newPassword) {
        userService.changePassword(userId, oldPassword, newPassword);
    }

    public Map<String, Object> getComments(Long userId) {
        User user = getUser(userId);

        Map<String, Object> comments = new HashMap<>();
        comments.put("spotComments", commentRepository.findByUserOrderByCreatedAtDesc(user));
        comments.put("routeComments", routeCommentRepository.findByUserOrderByCreatedAtDesc(user));
        return comments;
    }

    @Transactional
    public String uploadAvatar(Long userId, MultipartFile file) {
        String avatarUrl;
        try {
            avatarUrl = fileStorageService.storeAvatar(file);
        } catch (IOException exception) {
            throw new BusinessException("Avatar upload failed");
        }
        User user = getUser(userId);
        user.setAvatar(avatarUrl);
        userRepository.save(user);
        return avatarUrl;
    }

    @Transactional
    public String updateNickname(Long userId, String rawNickname) {
        String nickname = InputSanitizer.requiredPlainText(rawNickname, 32, "nickname");

        if (userRepository.existsByNickname(nickname)) {
            User existingNicknameUser = userRepository.findByNickname(nickname).orElse(null);
            if (existingNicknameUser != null && !existingNicknameUser.getId().equals(userId)) {
                throw new DataIntegrityViolationException("Nickname already exists");
            }
        }

        User user = getUser(userId);
        user.setNickname(nickname);
        userRepository.saveAndFlush(user);
        return nickname;
    }

    @Transactional
    public String updateAvatar(Long userId, String rawAvatarUrl) {
        String avatarUrl = InputSanitizer.optionalPublicImageUrl(rawAvatarUrl, "avatarUrl");
        User user = getUser(userId);
        user.setAvatar(avatarUrl);
        userRepository.save(user);
        return avatarUrl;
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
