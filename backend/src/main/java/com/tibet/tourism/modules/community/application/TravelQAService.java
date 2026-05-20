package com.tibet.tourism.modules.community.application;
import com.tibet.tourism.common.error.BusinessException;
import com.tibet.tourism.common.error.ResourceNotFoundException;
import com.tibet.tourism.common.error.UnauthorizedActionException;
import com.tibet.tourism.common.validation.InputSanitizer;
import com.tibet.tourism.modules.community.domain.QuestionLike;
import com.tibet.tourism.modules.community.domain.TravelAnswer;
import com.tibet.tourism.modules.community.domain.TravelQuestion;
import com.tibet.tourism.modules.community.infra.QuestionLikeRepository;
import com.tibet.tourism.modules.community.infra.TravelAnswerRepository;
import com.tibet.tourism.modules.community.infra.TravelQuestionRepository;
import com.tibet.tourism.modules.hotel.domain.Hotel;
import com.tibet.tourism.modules.user.domain.User;
import com.tibet.tourism.modules.user.infra.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.util.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class TravelQAService {

    @Autowired
    private TravelQuestionRepository questionRepository;

    @Autowired
    private TravelAnswerRepository answerRepository;

    @Autowired
    private QuestionLikeRepository likeRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public TravelQuestion askQuestion(Long userId, String title, String content, String tags) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TravelQuestion question = new TravelQuestion();
        question.setAuthor(user);
        question.setTitle(InputSanitizer.requiredPlainText(title, 200, "问题标题"));
        question.setContent(InputSanitizer.requiredTextBlock(content, 4000, "问题内容"));
        question.setTags(InputSanitizer.optionalTags(tags, 500));

        return questionRepository.save(question);
    }

    public Page<TravelQuestion> getQuestions(String tag, String sort, String status, Pageable pageable) {
        String safeTag = InputSanitizer.optionalTagFilter(tag);
        return questionRepository.findAll((Specification<TravelQuestion>) (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(safeTag)) {
                predicates.add(criteriaBuilder.like(root.get("tags"), "%" + safeTag + "%"));
            }

            if ("unsolved".equals(status)) {
                predicates.add(criteriaBuilder.isFalse(root.get("isResolved")));
            } else if ("solved".equals(status)) {
                predicates.add(criteriaBuilder.isTrue(root.get("isResolved")));
            }

            if ("hot".equals(sort)) {
                query.orderBy(criteriaBuilder.desc(root.get("answerCount")), criteriaBuilder.desc(root.get("createdAt")));
            } else if ("unsolved".equals(sort)) {
                query.orderBy(criteriaBuilder.asc(root.get("isResolved")), criteriaBuilder.desc(root.get("createdAt")));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        }, pageable);
    }

    @Transactional
    public TravelQuestion getQuestion(Long id) {
        TravelQuestion question = questionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        question.incrementViewCount();
        return questionRepository.save(question);
    }

    @Transactional
    public void deleteQuestion(Long questionId, Long userId) {
        TravelQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        if (!question.getAuthor().getId().equals(userId)) {
            throw new UnauthorizedActionException("Unauthorized: You can only delete your own questions");
        }

        likeRepository.deleteByQuestion(question);
        answerRepository.deleteByQuestion(question);
        questionRepository.delete(question);
    }

    @Transactional
    public TravelAnswer answerQuestion(Long questionId, Long userId, String content) {
        TravelQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        TravelAnswer answer = new TravelAnswer();
        answer.setQuestion(question);
        answer.setUser(user);
        answer.setContent(InputSanitizer.requiredTextBlock(content, 4000, "回答内容"));

        TravelAnswer saved = answerRepository.save(answer);

        question.incrementAnswerCount();
        questionRepository.save(question);

        return saved;
    }

    @Transactional
    public TravelAnswer acceptAnswer(Long questionId, Long answerId, Long userId) {
        TravelQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        if (!question.getAuthor().getId().equals(userId)) {
            throw new UnauthorizedActionException("Only the question author can accept an answer");
        }

        TravelAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new ResourceNotFoundException("Answer not found"));

        if (!answer.getQuestion().getId().equals(questionId)) {
            throw new BusinessException("Answer does not belong to this question");
        }

        answer.setIsAccepted(true);
        question.setIsResolved(true);

        questionRepository.save(question);
        return answerRepository.save(answer);
    }

    @Transactional
    public boolean likeQuestion(Long questionId, Long userId) {
        TravelQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        if (likeRepository.existsByQuestionAndUser(question, user)) {
            return false;
        }

        QuestionLike like = new QuestionLike();
        like.setQuestion(question);
        like.setUser(user);
        likeRepository.save(like);

        question.incrementLikeCount();
        questionRepository.save(question);
        return true;
    }

    @Transactional
    public boolean unlikeQuestion(Long questionId, Long userId) {
        TravelQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Optional<QuestionLike> like = likeRepository.findByQuestionAndUser(question, user);
        if (like.isEmpty()) {
            return false;
        }

        likeRepository.delete(like.get());
        question.decrementLikeCount();
        questionRepository.save(question);
        return true;
    }

    public boolean isLikedByUser(Long questionId, Long userId) {
        TravelQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return likeRepository.existsByQuestionAndUser(question, user);
    }

    public List<TravelAnswer> getAnswers(Long questionId) {
        TravelQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("Question not found"));
        return answerRepository.findByQuestionOrderByIsAcceptedDescLikeCountDescCreatedAtAsc(question);
    }

    public List<TravelQuestion> getQuestionsByAuthor(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return questionRepository.findByAuthorOrderByCreatedAtDesc(user);
    }
}
