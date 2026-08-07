package com.flakomencia.agendaflow.common.security;

import java.io.IOException;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.server.resource.InvalidBearerTokenException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class BearerAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final SecurityErrorResponseWriter writer;

    public BearerAuthenticationEntryPoint(SecurityErrorResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException exception) throws IOException, ServletException {
        if (exception instanceof InvalidBearerTokenException) {
            String details = exceptionChain(exception).toLowerCase(Locale.ROOT);
            if (details.contains("expired")) {
                writer.write(request, response, HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED", "The access token has expired");
            } else {
                writer.write(request, response, HttpStatus.UNAUTHORIZED, "INVALID_TOKEN", "The access token is invalid");
            }
            return;
        }
        writer.write(
                request, response, HttpStatus.UNAUTHORIZED,
                "AUTHENTICATION_REQUIRED", "Authentication is required to access this resource");
    }

    private String exceptionChain(Throwable throwable) {
        StringBuilder result = new StringBuilder();
        Throwable current = throwable;
        while (current != null) {
            result.append(' ').append(current.getMessage());
            current = current.getCause();
        }
        return result.toString();
    }
}
