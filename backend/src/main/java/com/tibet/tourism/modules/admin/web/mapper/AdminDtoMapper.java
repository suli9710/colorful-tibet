package com.tibet.tourism.modules.admin.web.mapper;
import com.tibet.tourism.modules.community.domain.Comment;
import com.tibet.tourism.modules.community.domain.RouteComment;
import com.tibet.tourism.modules.community.domain.SharedRoute;
import com.tibet.tourism.modules.community.domain.TravelAnswer;
import com.tibet.tourism.modules.community.domain.TravelQuestion;
import com.tibet.tourism.modules.user.domain.User;
import java.util.HashMap;
import java.util.Map;

public final class AdminDtoMapper {

    private AdminDtoMapper() {}

    public static Map<String, Object> toAdminSharedRoute(SharedRoute route) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", route.getId());
        data.put("title", route.getTitle());
        data.put("name", route.getTitle());
        data.put("content", route.getContent());
        data.put("description", route.getContent());
        data.put("days", route.getDays());
        data.put("budget", route.getBudget());
        data.put("preference", route.getPreference());
        data.put("sourceType", route.getSourceType());
        data.put("sourceRouteId", route.getSourceRouteId());
        data.put("price", route.getPrice());
        data.put("difficulty", route.getDifficulty());
        data.put("temperature", route.getTemperature());
        data.put("geography", route.getGeography());
        data.put("viewCount", route.getViewCount());
        data.put("likeCount", route.getLikeCount());
        data.put("commentCount", route.getCommentCount());
        data.put("createdAt", route.getCreatedAt());
        data.put("updatedAt", route.getUpdatedAt());
        data.put("author", toUserSummary(route.getAuthor()));
        return data;
    }

    public static Map<String, Object> toAdminRouteComment(RouteComment comment) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", comment.getId());
        data.put("content", comment.getContent());
        data.put("createdAt", comment.getCreatedAt());
        data.put("user", toUserSummary(comment.getUser()));
        if (comment.getRoute() != null) {
            data.put("routeId", comment.getRoute().getId());
            data.put("routeTitle", comment.getRoute().getTitle());
        }
        return data;
    }

    public static Map<String, Object> toAdminSpotComment(Comment comment) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", comment.getId());
        data.put("content", comment.getContent());
        data.put("rating", comment.getRating());
        data.put("imageUrl", comment.getImageUrl());
        data.put("likeCount", comment.getLikeCount());
        data.put("createdAt", comment.getCreatedAt());
        data.put("user", toUserSummary(comment.getUser()));
        if (comment.getSpot() != null) {
            data.put("spotId", comment.getSpot().getId());
            data.put("spotName", comment.getSpot().getName());
        }
        return data;
    }

    public static Map<String, Object> toAdminQuestion(TravelQuestion question) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", question.getId());
        data.put("title", question.getTitle());
        data.put("content", question.getContent());
        data.put("tags", question.getTags());
        data.put("viewCount", question.getViewCount());
        data.put("answerCount", question.getAnswerCount());
        data.put("likeCount", question.getLikeCount());
        data.put("isResolved", question.getIsResolved());
        data.put("createdAt", question.getCreatedAt());
        data.put("updatedAt", question.getUpdatedAt());
        data.put("author", toUserSummary(question.getAuthor()));
        return data;
    }

    public static Map<String, Object> toAdminAnswer(TravelAnswer answer) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", answer.getId());
        data.put("content", answer.getContent());
        data.put("likeCount", answer.getLikeCount());
        data.put("isAccepted", answer.getIsAccepted());
        data.put("createdAt", answer.getCreatedAt());
        data.put("user", toUserSummary(answer.getUser()));
        if (answer.getQuestion() != null) {
            data.put("questionId", answer.getQuestion().getId());
            data.put("questionTitle", answer.getQuestion().getTitle());
        }
        return data;
    }

    public static Map<String, Object> toUserSummary(User user) {
        Map<String, Object> data = new HashMap<>();
        if (user == null) {
            return data;
        }
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("avatar", user.getAvatar());
        data.put("role", user.getRole());
        return data;
    }
}
