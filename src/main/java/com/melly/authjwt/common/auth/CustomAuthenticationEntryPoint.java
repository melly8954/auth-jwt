package com.melly.authjwt.common.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melly.authjwt.common.dto.ResponseDto;
import com.melly.authjwt.common.enums.ErrorType;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        ResponseDto<?> dto = new ResponseDto<>(HttpServletResponse.SC_UNAUTHORIZED,
                ErrorType.UNAUTHORIZED.getErrorCode(),
                ErrorType.UNAUTHORIZED.getMessage(),
                null);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(dto));
    }
}
