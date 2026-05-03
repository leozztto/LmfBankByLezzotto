package com.lezztto.LmfBank.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lezztto.LmfBank.exception.domain.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException ex
    ) throws IOException {

        ApiError error = ApiError.builder()
                .status(401)
                .code("UNAUTHORIZED")
                .message("Missing or invalid Authorization header")
                .path(request.getRequestURI())
                .timestamp(LocalDateTime.now())
                .build();

        response.setStatus(401);
        response.setContentType("application/json");

        new ObjectMapper().writeValue(response.getOutputStream(), error);
    }
}
