package com.melly.authjwt.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class OAuth2LoginResponseDto {
    private String username;
    private String accessToken;
    private String refreshToken;
    private String role;
    private String message;
    private boolean success;
    private String provider; // GOOGLE, KAKAO 등
    private String providerId; // OAuth2 유저 고유 ID
}
