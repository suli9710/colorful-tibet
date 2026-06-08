package com.tibet.tourism.modules.auth.application;

import java.util.Map;

public record LoginResult(String jwt, String csrfToken, Map<String, Object> user) {
}
