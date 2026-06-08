package com.tibet.tourism.modules.user.application;
import com.tibet.tourism.common.error.BusinessException;
import com.tibet.tourism.common.error.ResourceNotFoundException;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.community.web.dto.CommentDTO;
import com.tibet.tourism.modules.community.web.dto.RouteCommentResponse;
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
import org.springframework.util.StringUtils;
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
        String nickname = publicNickname(user);
        if (nickname != null) {
            response.put("nickname", nickname);
        }
        response.put("avatar", user.getAvatar());
        response.put("avatarUrl", user.getAvatar());
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
        comments.put("spotComments", commentRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(CommentDTO::fromEntity)
                .toList());
        comments.put("routeComments", routeCommentRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(RouteCommentResponse::fromEntity)
                .toList());
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

    private String publicNickname(User user) {
        String nickname = user.getNickname();
        if (!StringUtils.hasText(nickname)) {
            return null;
        }
        String normalizedNickname = nickname.trim();
        String username = user.getUsername();
        if (StringUtils.hasText(username) && normalizedNickname.equalsIgnoreCase(username.trim())) {
            return null;
        }
        return normalizedNickname;
    }
}
