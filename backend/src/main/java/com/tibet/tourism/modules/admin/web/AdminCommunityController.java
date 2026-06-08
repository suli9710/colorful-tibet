package com.tibet.tourism.modules.admin.web;
import com.tibet.tourism.common.api.PageResponse;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.admin.web.dto.CommunityAnswerUpdateRequest;
import com.tibet.tourism.modules.admin.web.dto.CommunityContentUpdateRequest;
import com.tibet.tourism.modules.admin.web.dto.CommunityQuestionUpdateRequest;
import com.tibet.tourism.modules.admin.web.dto.CommunityRouteUpdateRequest;
import com.tibet.tourism.modules.admin.web.dto.CommunitySpotCommentUpdateRequest;
import com.tibet.tourism.modules.community.domain.Comment;
import com.tibet.tourism.modules.community.domain.RouteComment;
import com.tibet.tourism.modules.community.domain.SharedRoute;
import com.tibet.tourism.modules.community.domain.TravelAnswer;
import com.tibet.tourism.modules.community.domain.TravelQuestion;
import com.tibet.tourism.modules.community.infra.CommentLikeRepository;
import com.tibet.tourism.modules.community.infra.CommentRepository;
import com.tibet.tourism.modules.community.infra.QuestionLikeRepository;
import com.tibet.tourism.modules.community.infra.RouteCommentRepository;
import com.tibet.tourism.modules.community.infra.RouteLikeRepository;
import com.tibet.tourism.modules.community.infra.SharedRouteRepository;
import com.tibet.tourism.modules.community.infra.TravelAnswerRepository;
import com.tibet.tourism.modules.community.infra.TravelQuestionRepository;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import static com.tibet.tourism.common.validation.RequestParseUtils.isBlank;
import static com.tibet.tourism.modules.admin.web.mapper.AdminDtoMapper.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminCommunityController {

    private static final Set<String> ALLOWED_ROUTE_BUDGETS = Set.of("经济型", "舒适型", "豪华型");
    private static final Set<String> ALLOWED_ROUTE_PREFERENCES = Set.of("自然风光", "人文历史", "深度摄影", "休闲度假");

    private final SharedRouteRepository sharedRouteRepository;
    private final RouteLikeRepository routeLikeRepository;
    private final RouteCommentRepository routeCommentRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final TravelQuestionRepository travelQuestionRepository;
    private final TravelAnswerRepository travelAnswerRepository;
    private final QuestionLikeRepository questionLikeRepository;

    public AdminCommunityController(SharedRouteRepository sharedRouteRepository,
                                    RouteLikeRepository routeLikeRepository,
                                    RouteCommentRepository routeCommentRepository,
                                    CommentRepository commentRepository,
                                    CommentLikeRepository commentLikeRepository,
                                    TravelQuestionRepository travelQuestionRepository,
                                    TravelAnswerRepository travelAnswerRepository,
                                    QuestionLikeRepository questionLikeRepository) {
        this.sharedRouteRepository = sharedRouteRepository;
        this.routeLikeRepository = routeLikeRepository;
        this.routeCommentRepository = routeCommentRepository;
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.travelQuestionRepository = travelQuestionRepository;
        this.travelAnswerRepository = travelAnswerRepository;
        this.questionLikeRepository = questionLikeRepository;
    }

    @GetMapping("/community/routes")
    public ResponseEntity<PageResponse<Map<String, Object>>> getCommunityRoutes(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(sharedRouteRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(route -> toAdminSharedRoute(route))));
    }

    @PutMapping("/community/routes/{id}")
    @Transactional
    public ResponseEntity<?> updateCommunityRoute(@PathVariable Long id, @Valid @RequestBody CommunityRouteUpdateRequest request) {
        Optional<SharedRoute> routeOpt = sharedRouteRepository.findById(id);
        if (routeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        SharedRoute route = routeOpt.get();
        if (request.getTitle() != null) {
            if (isBlank(request.getTitle())) {
                return ResponseEntity.badRequest().body(Map.of("error", "标题不能为空"));
            }
            route.setTitle(InputSanitizer.requiredPlainText(request.getTitle(), 200, "标题"));
        }
        if (request.getContent() != null) {
            if (isBlank(request.getContent())) {
                return ResponseEntity.badRequest().body(Map.of("error", "内容不能为空"));
            }
            route.setContent(InputSanitizer.requiredTextBlock(request.getContent(), 12000, "内容"));
        }
        if (request.getDays() != null) {
            Integer days = request.getDays();
            if (days < 1 || days > 15) {
                return ResponseEntity.badRequest().body(Map.of("error", "天数必须在1到15之间"));
            }
            route.setDays(days);
        }
        if (request.getBudget() != null) {
            route.setBudget(InputSanitizer.optionalAllowedValue(
                    request.getBudget(), ALLOWED_ROUTE_BUDGETS, "预算"));
        }
        if (request.getPreference() != null) {
            route.setPreference(InputSanitizer.optionalAllowedValue(
                    request.getPreference(), ALLOWED_ROUTE_PREFERENCES, "旅行偏好"));
        }

        return ResponseEntity.ok(toAdminSharedRoute(sharedRouteRepository.save(route)));
    }

    @DeleteMapping("/community/routes/{id}")
    @Transactional
    public ResponseEntity<?> deleteCommunityRoute(@PathVariable Long id) {
        Optional<SharedRoute> routeOpt = sharedRouteRepository.findById(id);
        if (routeOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        deleteSharedRouteWithChildren(routeOpt.get());
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    @GetMapping("/community/comments")
    public ResponseEntity<PageResponse<Map<String, Object>>> getCommunityComments(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(routeCommentRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(comment -> toAdminRouteComment(comment))));
    }

    @PutMapping("/community/comments/{id}")
    @Transactional
    public ResponseEntity<?> updateCommunityComment(@PathVariable Long id, @Valid @RequestBody CommunityContentUpdateRequest request) {
        Optional<RouteComment> commentOpt = routeCommentRepository.findById(id);
        if (commentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        RouteComment comment = commentOpt.get();
        if (request.getContent() != null) {
            if (isBlank(request.getContent())) {
                return ResponseEntity.badRequest().body(Map.of("error", "内容不能为空"));
            }
            comment.setContent(InputSanitizer.requiredTextBlock(request.getContent(), 1000, "内容"));
        }

        return ResponseEntity.ok(toAdminRouteComment(routeCommentRepository.save(comment)));
    }

    @DeleteMapping("/community/comments/{id}")
    @Transactional
    public ResponseEntity<?> deleteCommunityComment(@PathVariable Long id) {
        Optional<RouteComment> commentOpt = routeCommentRepository.findById(id);
        if (commentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        RouteComment comment = commentOpt.get();
        SharedRoute route = comment.getRoute();
        routeCommentRepository.delete(comment);
        if (route != null) {
            route.decrementCommentCount();
            sharedRouteRepository.save(route);
        }
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    @GetMapping("/community/spot-comments")
    public ResponseEntity<PageResponse<Map<String, Object>>> getCommunitySpotComments(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(commentRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(comment -> toAdminSpotComment(comment))));
    }

    @PutMapping("/community/spot-comments/{id}")
    @Transactional
    public ResponseEntity<?> updateCommunitySpotComment(@PathVariable Long id, @Valid @RequestBody CommunitySpotCommentUpdateRequest request) {
        Optional<Comment> commentOpt = commentRepository.findById(id);
        if (commentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Comment comment = commentOpt.get();
        if (request.getContent() != null) {
            if (isBlank(request.getContent())) {
                return ResponseEntity.badRequest().body(Map.of("error", "内容不能为空"));
            }
            comment.setContent(InputSanitizer.requiredTextBlock(request.getContent(), 1000, "内容"));
        }
        if (request.getRating() != null) {
            int rating = request.getRating();
            if (rating < 1 || rating > 5) {
                return ResponseEntity.badRequest().body(Map.of("error", "评分必须在1到5之间"));
            }
            comment.setRating(rating);
        }

        return ResponseEntity.ok(toAdminSpotComment(commentRepository.save(comment)));
    }

    @DeleteMapping("/community/spot-comments/{id}")
    @Transactional
    public ResponseEntity<?> deleteCommunitySpotComment(@PathVariable Long id) {
        Optional<Comment> commentOpt = commentRepository.findById(id);
        if (commentOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        commentLikeRepository.deleteByCommentId(id);
        commentRepository.delete(commentOpt.get());
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    @GetMapping("/community/questions")
    public ResponseEntity<PageResponse<Map<String, Object>>> getCommunityQuestions(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(travelQuestionRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(question -> toAdminQuestion(question))));
    }

    @PutMapping("/community/questions/{id}")
    @Transactional
    public ResponseEntity<?> updateCommunityQuestion(@PathVariable Long id, @Valid @RequestBody CommunityQuestionUpdateRequest request) {
        Optional<TravelQuestion> questionOpt = travelQuestionRepository.findById(id);
        if (questionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TravelQuestion question = questionOpt.get();
        if (request.getTitle() != null) {
            if (isBlank(request.getTitle())) {
                return ResponseEntity.badRequest().body(Map.of("error", "标题不能为空"));
            }
            question.setTitle(InputSanitizer.requiredPlainText(request.getTitle(), 200, "标题"));
        }
        if (request.getContent() != null) {
            if (isBlank(request.getContent())) {
                return ResponseEntity.badRequest().body(Map.of("error", "内容不能为空"));
            }
            question.setContent(InputSanitizer.requiredTextBlock(request.getContent(), 4000, "内容"));
        }
        if (request.getTags() != null) {
            question.setTags(InputSanitizer.optionalTags(request.getTags(), 500));
        }
        if (request.getIsResolved() != null) {
            question.setIsResolved(request.getIsResolved());
        }

        return ResponseEntity.ok(toAdminQuestion(travelQuestionRepository.save(question)));
    }

    @DeleteMapping("/community/questions/{id}")
    @Transactional
    public ResponseEntity<?> deleteCommunityQuestion(@PathVariable Long id) {
        Optional<TravelQuestion> questionOpt = travelQuestionRepository.findById(id);
        if (questionOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TravelQuestion question = questionOpt.get();
        questionLikeRepository.deleteByQuestion(question);
        travelAnswerRepository.deleteByQuestion(question);
        travelQuestionRepository.delete(question);
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    @GetMapping("/community/answers")
    public ResponseEntity<PageResponse<Map<String, Object>>> getCommunityAnswers(
            @PageableDefault(size = 50) Pageable pageable) {
        return ResponseEntity.ok(PageResponse.from(travelAnswerRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(answer -> toAdminAnswer(answer))));
    }

    @PutMapping("/community/answers/{id}")
    @Transactional
    public ResponseEntity<?> updateCommunityAnswer(@PathVariable Long id, @Valid @RequestBody CommunityAnswerUpdateRequest request) {
        Optional<TravelAnswer> answerOpt = travelAnswerRepository.findById(id);
        if (answerOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TravelAnswer answer = answerOpt.get();
        if (request.getContent() != null) {
            if (isBlank(request.getContent())) {
                return ResponseEntity.badRequest().body(Map.of("error", "内容不能为空"));
            }
            answer.setContent(InputSanitizer.requiredTextBlock(request.getContent(), 4000, "内容"));
        }
        if (request.getIsAccepted() != null) {
            Boolean accepted = request.getIsAccepted();
            answer.setIsAccepted(accepted);

            TravelQuestion question = answer.getQuestion();
            if (question != null) {
                if (Boolean.TRUE.equals(accepted)) {
                    question.setIsResolved(true);
                } else if (!hasAcceptedAnswerExcept(question, answer.getId())) {
                    question.setIsResolved(false);
                }
                travelQuestionRepository.save(question);
            }
        }

        return ResponseEntity.ok(toAdminAnswer(travelAnswerRepository.save(answer)));
    }

    @DeleteMapping("/community/answers/{id}")
    @Transactional
    public ResponseEntity<?> deleteCommunityAnswer(@PathVariable Long id) {
        Optional<TravelAnswer> answerOpt = travelAnswerRepository.findById(id);
        if (answerOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        TravelAnswer answer = answerOpt.get();
        TravelQuestion question = answer.getQuestion();
        boolean wasAccepted = Boolean.TRUE.equals(answer.getIsAccepted());
        travelAnswerRepository.delete(answer);

        if (question != null) {
            question.decrementAnswerCount();
            if (wasAccepted && !hasAcceptedAnswerExcept(question, answer.getId())) {
                question.setIsResolved(false);
            }
            travelQuestionRepository.save(question);
        }
        return ResponseEntity.ok(Map.of("message", "删除成功"));
    }

    private void deleteSharedRouteWithChildren(SharedRoute route) {
        routeLikeRepository.deleteByRoute(route);
        routeCommentRepository.deleteByRoute(route);
        sharedRouteRepository.delete(route);
    }

    private boolean hasAcceptedAnswerExcept(TravelQuestion question, Long ignoredAnswerId) {
        return travelAnswerRepository.findByQuestionOrderByIsAcceptedDescLikeCountDescCreatedAtAsc(question)
                .stream()
                .anyMatch(answer -> Boolean.TRUE.equals(answer.getIsAccepted())
                        && (ignoredAnswerId == null || !ignoredAnswerId.equals(answer.getId())));
    }
}
