package com.tibet.tourism.dto;

public class AiRouteGenerateResponse {

    private String content;
    private String model;
    private String budget;
    private String preference;
    private Integer days;
    private String prompt;

    public AiRouteGenerateResponse() {
    }

    public AiRouteGenerateResponse(String content, String model, String budget, String preference, Integer days, String prompt) {
        this.content = content;
        this.model = model;
        this.budget = budget;
        this.preference = preference;
        this.days = days;
        this.prompt = prompt;
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

    public String getBudget() {
        return budget;
    }

    public void setBudget(String budget) {
        this.budget = budget;
    }

    public String getPreference() {
        return preference;
    }

    public void setPreference(String preference) {
        this.preference = preference;
    }

    public Integer getDays() {
        return days;
    }

    public void setDays(Integer days) {
        this.days = days;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
