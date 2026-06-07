package com.tibet.tourism.modules.content.web.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.Map;

public class DictionaryBatchAddRequest {

    @NotEmpty(message = "翻译数据不能为空")
    private Map<String, String> translations;

    private String type;

    public Map<String, String> getTranslations() { return translations; }
    public void setTranslations(Map<String, String> translations) { this.translations = translations; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
