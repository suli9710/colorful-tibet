package com.tibet.tourism.modules.ai.web.dto;

public class GuideChatResponse {

    private String content;
    private String model;
    private boolean fallback;
    private String action;
    private String actionLabel;
    private boolean limited;
    private Integer retryAfterSeconds;

    public GuideChatResponse() {
    }

    public GuideChatResponse(String content, String model, boolean fallback, String action, String actionLabel) {
        this.content = content;
        this.model = model;
        this.fallback = fallback;
        this.action = action;
        this.actionLabel = actionLabel;
    }

    public GuideChatResponse(String content, String model, boolean fallback, String action, String actionLabel,
                             boolean limited, Integer retryAfterSeconds) {
        this(content, model, fallback, action, actionLabel);
        this.limited = limited;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isFallback() {
        return fallback;
    }

    public void setFallback(boolean fallback) {
        this.fallback = fallback;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getActionLabel() {
        return actionLabel;
    }

    public void setActionLabel(String actionLabel) {
        this.actionLabel = actionLabel;
    }

    public boolean isLimited() {
        return limited;
    }

    public void setLimited(boolean limited) {
        this.limited = limited;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    public void setRetryAfterSeconds(Integer retryAfterSeconds) {
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
