package com.flakomencia.agendaflow.common.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class ApiAccessDeniedHandler implements AccessDeniedHandler {
    private final SecurityErrorResponseWriter writer;

    public ApiAccessDeniedHandler(SecurityErrorResponseWriter writer) {
        this.writer = writer;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception) throws IOException, ServletException {
        writer.write(request, response, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access to this resource is denied");
    }
}
