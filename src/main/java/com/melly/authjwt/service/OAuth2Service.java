package com.melly.authjwt.service;

import com.melly.authjwt.common.auth.PrincipalDetails;
import com.melly.authjwt.dto.response.OAuth2LoginResponseDto;
import jakarta.servlet.http.HttpServletRequest;

public interface OAuth2Service {
    OAuth2LoginResponseDto loginWithOAuth(PrincipalDetails principal, HttpServletRequest request, String registrationId);
}
