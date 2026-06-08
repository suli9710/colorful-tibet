package com.tibet.tourism.modules.user.web.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.tibet.tourism.common.validation.InputSanitizer;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateAvatarRequest(
        @NotBlank(message = "Avatar URL is required")
        @Size(max = 512, message = "Avatar URL length must not exceed 512 characters")
        String avatarUrl
) {

    @JsonIgnore
    @AssertTrue(message = "Avatar URL is invalid")
    public boolean isAvatarUrlAllowed() {
        try {
            InputSanitizer.optionalLocalAvatarResourcePath(avatarUrl, "avatarUrl");
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
