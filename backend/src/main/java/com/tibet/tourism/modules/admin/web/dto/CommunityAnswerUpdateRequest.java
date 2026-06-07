package com.tibet.tourism.modules.admin.web.dto;

import jakarta.validation.constraints.Size;

public class CommunityAnswerUpdateRequest {

    @Size(max = 4000, message = "内容长度不能超过4000个字符")
    private String content;

    private Boolean isAccepted;

    // Getters and Setters

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Boolean getIsAccepted() {
        return isAccepted;
    }

    public void setIsAccepted(Boolean isAccepted) {
        this.isAccepted = isAccepted;
    }
}
