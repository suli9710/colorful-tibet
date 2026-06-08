package com.tibet.tourism.common.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

final class SecurityErrorResponseWriter {

    private SecurityErrorResponseWriter() {
    }

    static void writeJson(HttpServletResponse response, int status, String error) throws IOException {
        writeJson(response, status, error, null);
    }

    static void writeJson(HttpServletResponse response, int status, String error, String message)
            throws IOException {
        if (response.isCommitted()) {
            return;
        }
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        response.setHeader(HttpHeaders.PRAGMA, "no-cache");
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.getWriter().write(body(error, message));
    }

    private static String body(String error, String message) {
        if (message == null || message.isBlank()) {
            return "{\"error\":\"" + escapeJson(error) + "\"}";
        }
        return "{\"error\":\"" + escapeJson(error) + "\",\"message\":\"" + escapeJson(message) + "\"}";
    }

    private static String escapeJson(String value) {
        String normalized = value == null ? "" : value;
        return normalized.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
