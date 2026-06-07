package com.tibet.tourism.modules.admin.web.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public class NewsRequest {

    private Boolean autoTranslate;

    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;

    @Size(max = 200, message = "藏语标题长度不能超过200个字符")
    private String titleTibetan;

    @Size(max = 20000, message = "内容长度不能超过20000个字符")
    private String content;

    @Size(max = 20000, message = "藏语内容长度不能超过20000个字符")
    private String contentTibetan;

    @Size(max = 50, message = "类别长度不能超过50个字符")
    private String category;

    @Size(max = 512, message = "图片地址长度不能超过512个字符")
    private String imageUrl;

    @Min(value = 0, message = "浏览量不能为负数")
    private Integer viewCount;

    // Getters and Setters

    public Boolean getAutoTranslate() {
        return autoTranslate;
    }

    public void setAutoTranslate(Boolean autoTranslate) {
        this.autoTranslate = autoTranslate;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getTitleTibetan() {
        return titleTibetan;
    }

    public void setTitleTibetan(String titleTibetan) {
        this.titleTibetan = titleTibetan;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getContentTibetan() {
        return contentTibetan;
    }

    public void setContentTibetan(String contentTibetan) {
        this.contentTibetan = contentTibetan;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Integer getViewCount() {
        return viewCount;
    }

    public void setViewCount(Integer viewCount) {
        this.viewCount = viewCount;
    }
}
