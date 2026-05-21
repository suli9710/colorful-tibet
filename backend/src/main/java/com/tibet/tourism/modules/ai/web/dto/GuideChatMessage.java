package com.tibet.tourism.modules.ai.web.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class GuideChatMessage {

    @Pattern(regexp = "^(user|guide)$", message = "消息角色不合法")
    private String role;

    @Size(max = 1000, message = "消息内容不能超过1000个字符")
    private String content;

    public GuideChatMessage() {
    }

    public GuideChatMessage(String role, String content) {
        this.role = role;
        this.content = content;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
