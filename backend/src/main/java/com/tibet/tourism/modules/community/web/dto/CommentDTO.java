package com.tibet.tourism.modules.community.web.dto;
import com.tibet.tourism.modules.community.domain.Comment;
import java.time.LocalDateTime;

public class CommentDTO {
    private Long id;
    private String content;
    private Integer rating;
    private String imageUrl;
    private Integer likeCount;
    private boolean liked;
    private boolean owner;
    private String nickname;
    private String avatar;
    private ParentSpotResponse spot;
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
    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }
    public boolean isOwner() { return owner; }
    public void setOwner(boolean owner) { this.owner = owner; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public ParentSpotResponse getSpot() { return spot; }
    public void setSpot(ParentSpotResponse spot) { this.spot = spot; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static CommentDTO fromEntity(Comment comment) {
        return fromEntity(comment, null);
    }

    public static CommentDTO fromEntity(Comment comment, Long currentUserId) {
        if (comment == null) throw new IllegalArgumentException("Comment cannot be null");
        CommentDTO dto = new CommentDTO();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        dto.setRating(comment.getRating());
        dto.setImageUrl(comment.getImageUrl());
        dto.setLikeCount(comment.getLikeCount());
        dto.setCreatedAt(comment.getCreatedAt());
        if (comment.getUser() != null) {
            PublicUserResponse publicUser = PublicUserResponse.fromEntity(comment.getUser(), currentUserId);
            dto.setOwner(publicUser.owner());
            dto.setNickname(publicUser.nickname());
            dto.setAvatar(publicUser.avatar());
        }
        if (comment.getSpot() != null) {
            dto.setSpot(new ParentSpotResponse(comment.getSpot().getId(), comment.getSpot().getName()));
        }
        return dto;
    }

    public record ParentSpotResponse(Long id, String name) {}
}
