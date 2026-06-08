package com.tibet.tourism.common.security;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.ServletException;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
public class AuthEntryPointJwt implements AuthenticationEntryPoint {

    private static final String ERROR = "Unauthorized";
    private static final String MESSAGE = "Authentication required";

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        SecurityErrorResponseWriter.writeJson(
                response,
                HttpServletResponse.SC_UNAUTHORIZED,
                ERROR,
                MESSAGE);
    }
}
