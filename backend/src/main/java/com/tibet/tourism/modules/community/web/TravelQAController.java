package com.tibet.tourism.modules.community.web;
import com.tibet.tourism.common.api.PageResponse;
import com.tibet.tourism.common.error.AuthenticationRequiredException;
import com.tibet.tourism.common.error.BusinessException;
import com.tibet.tourism.common.error.ResourceNotFoundException;
import com.tibet.tourism.common.error.UnauthorizedActionException;
import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.common.security.SensitiveLogSanitizer;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.community.application.TravelQAService;
import com.tibet.tourism.modules.community.domain.TravelAnswer;
import com.tibet.tourism.modules.community.domain.TravelQuestion;
import com.tibet.tourism.modules.community.web.dto.TravelAnswerResponse;
import com.tibet.tourism.modules.community.web.dto.TravelQuestionResponse;
import com.tibet.tourism.modules.community.web.dto.TravelQuestionSummaryResponse;
import com.tibet.tourism.modules.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/community/questions")
public class TravelQAController {

    private static final Logger logger = LoggerFactory.getLogger(TravelQAController.class);
    private static final String ERROR_AUTHENTICATION_REQUIRED = "Authentication required";
    private static final String ERROR_PERMISSION_DENIED = "Permission denied";
    private static final String ERROR_NOT_FOUND = "Resource not found";
    private static final String ERROR_INVALID_REQUEST = "Invalid request";
    private static final int DEFAULT_ANSWER_PAGE_SIZE = 20;
    private static final int MAX_ANSWER_PAGE_SIZE = 50;
    private static final Set<String> ALLOWED_ANSWER_SORT_FIELDS = Set.of();
    private static final Sort DEFAULT_ANSWER_SORT = Sort.by(
            Sort.Order.desc("isAccepted"),
            Sort.Order.desc("likeCount"),
            Sort.Order.asc("createdAt"),
            Sort.Order.asc("id"));

    @Autowired
    private TravelQAService qaService;

    @Autowired
    private JwtAuthSupport jwtAuthSupport;

    private long getCurrentUserId(HttpServletRequest request) {
        try {
            return jwtAuthSupport.resolveCurrentUserId(request);
        } catch (UsernameNotFoundException e) {
            throw new ResourceNotFoundException("User not found");
        } catch (IllegalStateException e) {
            throw new AuthenticationRequiredException("User not authenticated");
        }
    }

    private Long getOptionalCurrentUserId(HttpServletRequest request) {
        try {
            Optional<User> currentUser = jwtAuthSupport.resolveOptionalCurrentUser(request);
            return currentUser == null ? null : currentUser.map(User::getId).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    private ResponseEntity<Map<String, String>> safeBadRequest(Exception e) {
        logger.warn("Community question request failed: {}", SensitiveLogSanitizer.exceptionSummary(e));
        if (e instanceof AuthenticationRequiredException) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", ERROR_AUTHENTICATION_REQUIRED));
        }
        if (e instanceof UnauthorizedActionException || e instanceof SecurityException) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", ERROR_PERMISSION_DENIED));
        }
        if (e instanceof ResourceNotFoundException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", ERROR_NOT_FOUND));
        }
        if (e instanceof BusinessException || e instanceof IllegalArgumentException) {
            return ResponseEntity.badRequest().body(Map.of("error", ERROR_INVALID_REQUEST));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "请求处理失败，请检查输入后重试"));
    }

    private <T> ResponseEntity<List<T>> pagedContent(Page<T> page) {
        return ResponseEntity.ok()
                .header("X-Page", String.valueOf(page.getNumber()))
                .header("X-Size", String.valueOf(page.getSize()))
                .header("X-Total-Elements", String.valueOf(page.getTotalElements()))
                .header("X-Total-Pages", String.valueOf(page.getTotalPages()))
                .body(page.getContent());
    }

    // 提问
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> askQuestion(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        try {
            long userId = getCurrentUserId(request);
            TravelQuestion question = qaService.askQuestion(
                    userId,
                    payload.get("title"),
                    payload.get("content"),
                    payload.get("tags")
            );
            return ResponseEntity.ok(TravelQuestionResponse.fromEntity(question, userId));
        } catch (Exception e) {
            return safeBadRequest(e);
        }
    }

    // 问题列表
    @GetMapping
    public ResponseEntity<?> getQuestions(
            @RequestParam(required = false) String tag,
            @RequestParam(required = false, defaultValue = "latest") String sort,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest request) {

        Sort sorting;
        if ("hot".equals(sort)) {
            sorting = Sort.by(Sort.Direction.DESC, "answerCount", "createdAt");
        } else if ("oldest".equals(sort)) {
            sorting = Sort.by(Sort.Direction.ASC, "createdAt");
        } else if ("unsolved".equals(sort)) {
            sorting = Sort.by(Sort.Order.asc("isResolved"), Sort.Order.desc("createdAt"));
        } else {
            sorting = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        Pageable pageable = PageRequest.of(
                InputSanitizer.normalizePage(page),
                InputSanitizer.normalizePageSize(size, 10, 50),
                sorting);
        Long currentUserId = getOptionalCurrentUserId(request);
        Page<TravelQuestionSummaryResponse> questions = qaService.getQuestionSummaries(
                tag, status, pageable, currentUserId);
        return ResponseEntity.ok(PageResponse.from(questions));
    }

    // 问题详情
    @GetMapping("/{id}")
    public ResponseEntity<?> getQuestion(@PathVariable Long id, HttpServletRequest request) {
        try {
            TravelQuestion question = qaService.getQuestion(id);
            return ResponseEntity.ok(TravelQuestionResponse.fromEntity(question, getOptionalCurrentUserId(request)));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 删除问题
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long id, HttpServletRequest request) {
        try {
            qaService.deleteQuestion(id, getCurrentUserId(request));
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return safeBadRequest(e);
        }
    }

    // 回答问题
    @PostMapping("/{id}/answers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> answerQuestion(@PathVariable Long id, @RequestBody Map<String, String> payload, HttpServletRequest request) {
        try {
            long userId = getCurrentUserId(request);
            TravelAnswer answer = qaService.answerQuestion(
                    id,
                    userId,
                    payload.get("content")
            );
            return ResponseEntity.ok(TravelAnswerResponse.fromEntity(answer, userId));
        } catch (Exception e) {
            return safeBadRequest(e);
        }
    }

    // 获取回答列表
    @GetMapping("/{id}/answers")
    public ResponseEntity<?> getAnswers(
            @PathVariable Long id,
            @PageableDefault(size = DEFAULT_ANSWER_PAGE_SIZE) Pageable pageable,
            HttpServletRequest request) {
        try {
            Long currentUserId = getOptionalCurrentUserId(request);
            Pageable safePageable = InputSanitizer.sanitizePageable(
                    pageable,
                    ALLOWED_ANSWER_SORT_FIELDS,
                    DEFAULT_ANSWER_SORT,
                    DEFAULT_ANSWER_PAGE_SIZE,
                    MAX_ANSWER_PAGE_SIZE);
            Page<TravelAnswerResponse> answers = qaService.getAnswers(id, safePageable)
                    .map(answer -> TravelAnswerResponse.fromEntity(answer, currentUserId));
            return pagedContent(answers);
        } catch (Exception e) {
            return safeBadRequest(e);
        }
    }

    // 采纳回答
    @PostMapping("/{questionId}/answers/{answerId}/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> acceptAnswer(@PathVariable Long questionId, @PathVariable Long answerId, HttpServletRequest request) {
        try {
            long userId = getCurrentUserId(request);
            TravelAnswer answer = qaService.acceptAnswer(questionId, answerId, userId);
            return ResponseEntity.ok(TravelAnswerResponse.fromEntity(answer, userId));
        } catch (Exception e) {
            return safeBadRequest(e);
        }
    }

    // 点赞问题
    @PostMapping("/{id}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> likeQuestion(@PathVariable Long id, HttpServletRequest request) {
        try {
            TravelQAService.LikeResult result = qaService.likeQuestion(id, getCurrentUserId(request));
            return ResponseEntity.ok(Map.of("liked", result.liked(), "likeCount", result.likeCount()));
        } catch (Exception e) {
            return safeBadRequest(e);
        }
    }

    // 取消点赞
    @DeleteMapping("/{id}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> unlikeQuestion(@PathVariable Long id, HttpServletRequest request) {
        try {
            TravelQAService.LikeResult result = qaService.unlikeQuestion(id, getCurrentUserId(request));
            return ResponseEntity.ok(Map.of("liked", result.liked(), "likeCount", result.likeCount()));
        } catch (Exception e) {
            return safeBadRequest(e);
        }
    }

    // 检查点赞状态
    @GetMapping("/{id}/like-status")
    public ResponseEntity<?> checkLikeStatus(@PathVariable Long id, HttpServletRequest request) {
        try {
            Optional<User> currentUser = jwtAuthSupport.resolveOptionalCurrentUser(request);
            boolean isLiked = currentUser
                    .map(user -> qaService.isLikedByUser(id, user.getId()))
                    .orElse(false);
            return ResponseEntity.ok(Map.of("liked", isLiked));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("liked", false));
        }
    }
}
