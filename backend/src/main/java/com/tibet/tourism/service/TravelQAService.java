package com.tibet.tourism.service;

import com.tibet.tourism.entity.*;
import com.tibet.tourism.repository.*;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;

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
                .orElseThrow(() -> new RuntimeException("User not found"));

        TravelQuestion question = new TravelQuestion();
        question.setAuthor(user);
        question.setTitle(title);
        question.setContent(content);
        question.setTags(tags);

        return questionRepository.save(question);
    }

    public Page<TravelQuestion> getQuestions(String tag, String sort, String status, Pageable pageable) {
        return questionRepository.findAll((Specification<TravelQuestion>) (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(tag)) {
                predicates.add(criteriaBuilder.like(root.get("tags"), "%" + tag + "%"));
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
                .orElseThrow(() -> new RuntimeException("Question not found"));
        question.incrementViewCount();
        return questionRepository.save(question);
    }

    @Transactional
    public void deleteQuestion(Long questionId, Long userId) {
        TravelQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        if (!question.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("Unauthorized: You can only delete your own questions");
        }

        questionRepository.delete(question);
    }

    @Transactional
    public TravelAnswer answerQuestion(Long questionId, Long userId, String content) {
        TravelQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        TravelAnswer answer = new TravelAnswer();
        answer.setQuestion(question);
        answer.setUser(user);
        answer.setContent(content);

        TravelAnswer saved = answerRepository.save(answer);

        question.incrementAnswerCount();
        questionRepository.save(question);

        return saved;
    }

    @Transactional
    public TravelAnswer acceptAnswer(Long questionId, Long answerId, Long userId) {
        TravelQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        if (!question.getAuthor().getId().equals(userId)) {
            throw new RuntimeException("Only the question author can accept an answer");
        }

        TravelAnswer answer = answerRepository.findById(answerId)
                .orElseThrow(() -> new RuntimeException("Answer not found"));

        if (!answer.getQuestion().getId().equals(questionId)) {
            throw new RuntimeException("Answer does not belong to this question");
        }

        answer.setIsAccepted(true);
        question.setIsResolved(true);

        questionRepository.save(question);
        return answerRepository.save(answer);
    }

    @Transactional
    public boolean likeQuestion(Long questionId, Long userId) {
        TravelQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

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
                .orElseThrow(() -> new RuntimeException("Question not found"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

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
                .orElseThrow(() -> new RuntimeException("Question not found"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return likeRepository.existsByQuestionAndUser(question, user);
    }

    public List<TravelAnswer> getAnswers(Long questionId) {
        TravelQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Question not found"));
        return answerRepository.findByQuestionOrderByIsAcceptedDescLikeCountDescCreatedAtAsc(question);
    }

    public List<TravelQuestion> getQuestionsByAuthor(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return questionRepository.findByAuthorOrderByCreatedAtDesc(user);
    }
}
