package com.tibet.tourism.modules.community.web.dto;
import com.tibet.tourism.modules.community.domain.Comment;
import java.time.LocalDateTime;

public class CommentDTO {
    private Long id;
    private String content;
    private Integer rating;
    private String imageUrl;
    private Integer likeCount;
    private Long userId;
    private String nickname;
    private String avatar;
    private PublicUserResponse user;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Integer getLikeCount() { return likeCount; }
    public void setLikeCount(Integer likeCount) { this.likeCount = likeCount; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public PublicUserResponse getUser() { return user; }
    public void setUser(PublicUserResponse user) { this.user = user; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static CommentDTO fromEntity(Comment comment) {
        if (comment == null) throw new IllegalArgumentException("Comment cannot be null");
        PublicUserResponse publicUser = PublicUserResponse.fromEntity(comment.getUser());
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setRating(comment.getRating());
        dto.setImageUrl(comment.getImageUrl());
        dto.setLikeCount(comment.getLikeCount());
        dto.setUser(publicUser);
        dto.setUserId(publicUser != null ? publicUser.id() : null);
        dto.setNickname(publicUser != null ? publicUser.nickname() : null);
        dto.setAvatar(publicUser != null ? publicUser.avatar() : null);
        dto.setCreatedAt(comment.getCreatedAt());
        return dto;
    }
}
