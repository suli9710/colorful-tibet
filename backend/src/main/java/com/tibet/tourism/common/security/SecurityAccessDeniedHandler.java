package com.tibet.tourism.common.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

public class SecurityAccessDeniedHandler implements AccessDeniedHandler {

    private static final String ERROR = "Forbidden";
    private static final String MESSAGE = "Access denied";

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {
        SecurityErrorResponseWriter.writeJson(
                response,
                HttpServletResponse.SC_FORBIDDEN,
                ERROR,
                MESSAGE);
    }
}
