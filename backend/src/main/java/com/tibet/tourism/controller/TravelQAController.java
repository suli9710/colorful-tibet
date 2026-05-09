package com.tibet.tourism.controller;

import com.tibet.tourism.entity.TravelAnswer;
import com.tibet.tourism.entity.TravelQuestion;
import com.tibet.tourism.entity.User;
import com.tibet.tourism.repository.UserRepository;
import com.tibet.tourism.security.JwtUtils;
import com.tibet.tourism.service.TravelQAService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/community/questions")
public class TravelQAController {

    @Autowired
    private TravelQAService qaService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtUtils jwtUtils;

    private long getCurrentUserId(HttpServletRequest request) {
        String jwt = parseJwt(request);
        if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
            String username = jwtUtils.getUserNameFromJwtToken(jwt);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            return user.getId();
        }
        throw new RuntimeException("User not authenticated");
    }

    private String parseJwt(HttpServletRequest request) {
        String headerAuth = request.getHeader("Authorization");
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            return headerAuth.substring(7);
        }
        return null;
    }

    // 提问
    @PostMapping
    public ResponseEntity<?> askQuestion(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        try {
            TravelQuestion question = qaService.askQuestion(
                    getCurrentUserId(request),
                    payload.get("title"),
                    payload.get("content"),
                    payload.get("tags")
            );
            return ResponseEntity.ok(question);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
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

        Pageable pageable = PageRequest.of(page, size, sorting);
        Page<TravelQuestion> questions = qaService.getQuestions(tag, sort, status, pageable);
        return ResponseEntity.ok(questions);
    }

    // 问题详情
    @GetMapping("/{id}")
    public ResponseEntity<?> getQuestion(@PathVariable Long id) {
        try {
            TravelQuestion question = qaService.getQuestion(id);
            return ResponseEntity.ok(question);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // 删除问题
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteQuestion(@PathVariable Long id, HttpServletRequest request) {
        try {
            qaService.deleteQuestion(id, getCurrentUserId(request));
            return ResponseEntity.noContent().build();
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 回答问题
    @PostMapping("/{id}/answers")
    public ResponseEntity<?> answerQuestion(@PathVariable Long id, @RequestBody Map<String, String> payload, HttpServletRequest request) {
        try {
            TravelAnswer answer = qaService.answerQuestion(
                    id,
                    getCurrentUserId(request),
                    payload.get("content")
            );
            return ResponseEntity.ok(answer);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 获取回答列表
    @GetMapping("/{id}/answers")
    public ResponseEntity<?> getAnswers(@PathVariable Long id) {
        try {
            List<TravelAnswer> answers = qaService.getAnswers(id);
            return ResponseEntity.ok(answers);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 采纳回答
    @PostMapping("/{questionId}/answers/{answerId}/accept")
    public ResponseEntity<?> acceptAnswer(@PathVariable Long questionId, @PathVariable Long answerId, HttpServletRequest request) {
        try {
            TravelAnswer answer = qaService.acceptAnswer(questionId, answerId, getCurrentUserId(request));
            return ResponseEntity.ok(answer);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 点赞问题
    @PostMapping("/{id}/like")
    public ResponseEntity<?> likeQuestion(@PathVariable Long id, HttpServletRequest request) {
        try {
            boolean liked = qaService.likeQuestion(id, getCurrentUserId(request));
            TravelQuestion question = qaService.getQuestion(id);
            return ResponseEntity.ok(Map.of("liked", liked, "likeCount", question.getLikeCount()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 取消点赞
    @DeleteMapping("/{id}/like")
    public ResponseEntity<?> unlikeQuestion(@PathVariable Long id, HttpServletRequest request) {
        try {
            boolean unliked = qaService.unlikeQuestion(id, getCurrentUserId(request));
            TravelQuestion question = qaService.getQuestion(id);
            return ResponseEntity.ok(Map.of("liked", !unliked, "likeCount", question.getLikeCount()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // 检查点赞状态
    @GetMapping("/{id}/like-status")
    public ResponseEntity<?> checkLikeStatus(@PathVariable Long id, HttpServletRequest request) {
        try {
            boolean isLiked = qaService.isLikedByUser(id, getCurrentUserId(request));
            return ResponseEntity.ok(Map.of("liked", isLiked));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("liked", false));
        }
    }
}
