package com.tibet.tourism.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 藏语词典实体
 * 用于存储中文到藏语的翻译映射，支持管理员编辑和管理
 */
@Entity
@Table(name = "tibetan_dictionary", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"chinese_text", "type"}))
public class TibetanDictionary {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 中文文本（词或短语）
     */
    @Column(name = "chinese_text", nullable = false, length = 500)
    private String chineseText;

    /**
     * 藏语翻译
     */
    @Column(name = "tibetan_text", nullable = false, columnDefinition = "TEXT")
    private String tibetanText;

    /**
     * 类型：WORD（单词）、PHRASE（短语）、SENTENCE（句子）
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Type type;

    /**
     * 使用频率（用于排序和推荐）
     */
    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;

    /**
     * 创建时间
     */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChineseText() {
        return chineseText;
    }

    public void setChineseText(String chineseText) {
        this.chineseText = chineseText;
    }

    public String getTibetanText() {
        return tibetanText;
    }

    public void setTibetanText(String tibetanText) {
        this.tibetanText = tibetanText;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public void incrementUsageCount() {
        this.usageCount = (this.usageCount == null ? 0 : this.usageCount) + 1;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public enum Type {
        WORD,      // 单词
        PHRASE,    // 短语
        SENTENCE   // 句子
    }
}




