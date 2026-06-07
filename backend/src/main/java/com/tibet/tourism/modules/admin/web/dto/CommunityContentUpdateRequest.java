package com.tibet.tourism.modules.admin.web.dto;

import jakarta.validation.constraints.Size;

/**
 * Reusable DTO for updates that only contain a {@code content} field.
 * Used by route-comment and similar single-text-field update endpoints.
 */
public class CommunityContentUpdateRequest {

    @Size(max = 12000, message = "内容长度不能超过12000个字符")
    private String content;

    // Getters and Setters

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
