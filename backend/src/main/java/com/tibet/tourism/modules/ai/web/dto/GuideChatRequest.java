package com.tibet.tourism.modules.ai.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

public class GuideChatRequest {

    @NotBlank(message = "消息不能为空")
    @Size(max = 500, message = "消息内容不能超过500个字符")
    private String message;

    @Size(max = 8, message = "上下文消息不能超过8条")
    private List<@Valid GuideChatMessage> history = new ArrayList<>();

    @Pattern(regexp = "^(zh|bo)?$", message = "语言参数只支持 zh 或 bo")
    private String locale;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<GuideChatMessage> getHistory() {
        return history;
    }

    public void setHistory(List<GuideChatMessage> history) {
        this.history = history == null ? new ArrayList<>() : history;
    }

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
}
