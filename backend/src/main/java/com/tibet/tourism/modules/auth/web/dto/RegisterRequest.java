package com.tibet.tourism.modules.auth.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 32, message = "Username length must be between 3 and 32 characters")
    @Pattern(
            regexp = "^[A-Za-z0-9_-]+$",
            message = "Username can only contain ASCII letters, numbers, underscores, or hyphens")
    private String username;

    @Size(max = 32, message = "Nickname length must not exceed 32 characters")
    private String nickname;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 72, message = "Password length must be between 8 and 72 characters")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).+$",
            message = "Password must contain lowercase, uppercase, number, and special characters")
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
