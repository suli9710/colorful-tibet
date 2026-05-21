package com.tibet.tourism.modules.community.web;
import com.tibet.tourism.common.error.AuthenticationRequiredException;
import com.tibet.tourism.common.error.BusinessException;
import com.tibet.tourism.common.error.ResourceNotFoundException;
import com.tibet.tourism.common.error.UnauthorizedActionException;
import com.tibet.tourism.common.security.JwtAuthSupport;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.community.application.TravelQAService;
import com.tibet.tourism.modules.community.domain.TravelAnswer;
import com.tibet.tourism.modules.community.domain.TravelQuestion;
import com.tibet.tourism.modules.community.web.dto.TravelAnswerResponse;
import com.tibet.tourism.modules.community.web.dto.TravelQuestionResponse;
import com.tibet.tourism.modules.user.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/community/questions")
public class TravelQAController {

    private static final Logger logger = LoggerFactory.getLogger(TravelQAController.class);

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

    private ResponseEntity<Map<String, String>> safeBadRequest(Exception e) {
        logger.warn("Community question request failed: {}", e.getMessage());
        if (e instanceof AuthenticationRequiredException) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("error", e.getMessage()));
        }
        if (e instanceof UnauthorizedActionException || e instanceof SecurityException) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        }
        if (e instanceof ResourceNotFoundException) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
        if (e instanceof BusinessException || e instanceof IllegalArgumentException) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "请求处理失败，请检查输入后重试"));
    }

    // 提问
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> askQuestion(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        try {
            TravelQuestion question = qaService.askQuestion(
                    getCurrentUserId(request),
                    payload.get("title"),
                    payload.get("content"),
                    payload.get("tags")
            );
            return ResponseEntity.ok(TravelQuestionResponse.fromEntity(question));
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
            @RequestParam(defaultValue = "10") int size) {

        Sort sorting;
        if ("hot".equals(sort)) {
            sorting = Sort.by(Sort.Direction.DESC, "answerCount", "createdAt");
        } else if ("oldest".equals(sort)) {
            sorting = Sort.by(Sort.Direction.ASC, "createdAt");
        } else {
            sorting = Sort.by(Sort.Direction.DESC, "createdAt");
        }

        Pageable pageable = PageRequest.of(
                InputSanitizer.normalizePage(page),
                InputSanitizer.normalizePageSize(size, 10, 50),
                sorting);
        Page<TravelQuestionResponse> questions = qaService.getQuestions(tag, sort, status, pageable)
                .map(TravelQuestionResponse::fromEntity);
        return ResponseEntity.ok(questions);
    }

    // 问题详情
    @GetMapping("/{id}")
    public ResponseEntity<?> getQuestion(@PathVariable Long id) {
        try {
            TravelQuestion question = qaService.getQuestion(id);
            return ResponseEntity.ok(TravelQuestionResponse.fromEntity(question));
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
            TravelAnswer answer = qaService.answerQuestion(
                    id,
                    getCurrentUserId(request),
                    payload.get("content")
            );
            return ResponseEntity.ok(TravelAnswerResponse.fromEntity(answer));
        } catch (Exception e) {
            return safeBadRequest(e);
        }
    }

    // 获取回答列表
    @GetMapping("/{id}/answers")
    public ResponseEntity<?> getAnswers(@PathVariable Long id) {
        try {
            List<TravelAnswerResponse> answers = qaService.getAnswers(id).stream()
                    .map(TravelAnswerResponse::fromEntity)
                    .toList();
            return ResponseEntity.ok(answers);
        } catch (Exception e) {
            return safeBadRequest(e);
        }
    }

    // 采纳回答
    @PostMapping("/{questionId}/answers/{answerId}/accept")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> acceptAnswer(@PathVariable Long questionId, @PathVariable Long answerId, HttpServletRequest request) {
        try {
            TravelAnswer answer = qaService.acceptAnswer(questionId, answerId, getCurrentUserId(request));
            return ResponseEntity.ok(TravelAnswerResponse.fromEntity(answer));
        } catch (Exception e) {
            return safeBadRequest(e);
        }
    }

    // 点赞问题
    @PostMapping("/{id}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> likeQuestion(@PathVariable Long id, HttpServletRequest request) {
        try {
            boolean liked = qaService.likeQuestion(id, getCurrentUserId(request));
            TravelQuestion question = qaService.getQuestion(id);
            return ResponseEntity.ok(Map.of("liked", liked, "likeCount", question.getLikeCount()));
        } catch (Exception e) {
            return safeBadRequest(e);
        }
    }

    // 取消点赞
    @DeleteMapping("/{id}/like")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> unlikeQuestion(@PathVariable Long id, HttpServletRequest request) {
        try {
            boolean unliked = qaService.unlikeQuestion(id, getCurrentUserId(request));
            TravelQuestion question = qaService.getQuestion(id);
            return ResponseEntity.ok(Map.of("liked", !unliked, "likeCount", question.getLikeCount()));
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
