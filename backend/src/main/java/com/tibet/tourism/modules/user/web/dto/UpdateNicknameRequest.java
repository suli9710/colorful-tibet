package com.tibet.tourism.modules.user.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateNicknameRequest(
        @NotBlank(message = "Nickname is required")
        @Size(max = 32, message = "Nickname length must not exceed 32 characters")
        String nickname
) {
}
