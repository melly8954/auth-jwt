package com.melly.authjwt.service;

import com.melly.authjwt.dto.request.LoginRequestDto;
import com.melly.authjwt.dto.response.LoginResponseDto;
import com.melly.authjwt.dto.response.ReIssueTokenDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface AuthService {
    LoginResponseDto login(LoginRequestDto dto, HttpServletResponse response);
    ReIssueTokenDto reissueToken(HttpServletRequest request, HttpServletResponse response);
    void logout(HttpServletRequest request, HttpServletResponse response);
}
