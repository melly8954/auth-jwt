package com.melly.authjwt.service;

import com.melly.authjwt.common.auth.PrincipalDetails;
import com.melly.authjwt.dto.response.OAuth2LoginResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public interface OAuth2Service {
    OAuth2LoginResponseDto loginWithOAuth(PrincipalDetails principal, HttpServletRequest request, HttpServletResponse response, String registrationId);
}
