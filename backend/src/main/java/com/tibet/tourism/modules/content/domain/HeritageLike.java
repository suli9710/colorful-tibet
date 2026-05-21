package com.tibet.tourism.modules.content.domain;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tibet.tourism.modules.user.domain.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "heritage_likes", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "heritage_item_id"})
}, indexes = {
    @Index(name = "idx_heritage_likes_item", columnList = "heritage_item_id"),
    @Index(name = "idx_heritage_likes_user", columnList = "user_id")
})
public class HeritageLike {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @JsonIgnore
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "heritage_item_id", nullable = false)
    @JsonIgnore
    private HeritageItem heritageItem;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public HeritageItem getHeritageItem() {
        return heritageItem;
    }

    public void setHeritageItem(HeritageItem heritageItem) {
        this.heritageItem = heritageItem;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
