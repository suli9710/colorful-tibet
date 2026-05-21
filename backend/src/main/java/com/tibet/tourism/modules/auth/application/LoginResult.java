package com.tibet.tourism.modules.auth.application;
import com.tibet.tourism.modules.user.domain.User;
import java.util.Map;

public record LoginResult(String jwt, String csrfToken, Map<String, Object> user) {
}
