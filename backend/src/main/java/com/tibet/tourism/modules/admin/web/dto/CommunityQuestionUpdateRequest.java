package com.tibet.tourism.modules.admin.web.dto;

import jakarta.validation.constraints.Size;

public class CommunityQuestionUpdateRequest {

    @Size(max = 200, message = "标题长度不能超过200个字符")
    private String title;

    @Size(max = 4000, message = "内容长度不能超过4000个字符")
    private String content;

    @Size(max = 500, message = "标签长度不能超过500个字符")
    private String tags;

    private Boolean isResolved;

    // Getters and Setters

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public Boolean getIsResolved() {
        return isResolved;
    }

    public void setIsResolved(Boolean isResolved) {
        this.isResolved = isResolved;
    }
}
