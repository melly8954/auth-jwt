package com.melly.authjwt.service;

import com.melly.authjwt.dto.request.LoginRequestDto;
import com.melly.authjwt.dto.response.LoginResponseDto;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    LoginResponseDto login(LoginRequestDto dto, HttpServletResponse response);
}
