package com.tibet.tourism.modules.content.web.dto;
import com.tibet.tourism.modules.content.domain.News;
import java.time.LocalDateTime;

public class NewsDTO {
    private Long id;
    private String title;
    private String content;
    private News.Category category;
    private String imageUrl;
    private Integer viewCount;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public News.Category getCategory() { return category; }
    public void setCategory(News.Category category) { this.category = category; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Integer getViewCount() { return viewCount; }
    public void setViewCount(Integer viewCount) { this.viewCount = viewCount; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static NewsDTO fromEntity(News news, String locale) {
        if (news == null) throw new IllegalArgumentException("News cannot be null");
        NewsDTO dto = new NewsDTO();
        dto.setId(news.getId());
        dto.setTitle("bo".equals(locale) && news.getTitleTibetan() != null && !news.getTitleTibetan().isEmpty()
                ? news.getTitleTibetan() : news.getTitle());
        dto.setContent("bo".equals(locale) && news.getContentTibetan() != null && !news.getContentTibetan().isEmpty()
                ? news.getContentTibetan() : news.getContent());
        dto.setCategory(news.getCategory());
        dto.setImageUrl(news.getImageUrl());
        dto.setViewCount(news.getViewCount());
        dto.setCreatedAt(news.getCreatedAt());
        return dto;
    }
}
