package com.tibet.tourism.modules.content.web.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class TibetanDictionaryUpdateRequest {

    @Size(max = 500)
    private String chineseText;

    @Size(max = 10000)
    private String tibetanText;

    @Size(max = 20)
    @Pattern(regexp = "(?i)^(WORD|PHRASE|SENTENCE)$")
    private String type;

    public String getChineseText() {
        return chineseText;
    }

    public void setChineseText(String chineseText) {
        this.chineseText = chineseText;
    }

    public String getTibetanText() {
        return tibetanText;
    }

    public void setTibetanText(String tibetanText) {
        this.tibetanText = tibetanText;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
