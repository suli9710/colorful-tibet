package com.tibet.tourism.modules.content.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.Map;

public class DictionaryBatchAddRequest {

    @NotEmpty
    @Size(max = 200)
    private Map<@NotBlank @Size(max = 500) String, @NotBlank @Size(max = 10000) String> translations;

    @Size(max = 20)
    @Pattern(regexp = "(?i)^(|WORD|PHRASE|SENTENCE)$")
    private String type;

    public Map<String, String> getTranslations() {
        return translations;
    }

    public void setTranslations(Map<String, String> translations) {
        this.translations = translations;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
