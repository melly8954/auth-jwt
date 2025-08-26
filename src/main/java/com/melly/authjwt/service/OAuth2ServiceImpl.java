package com.melly.authjwt.service;

import com.melly.authjwt.common.auth.PrincipalDetails;
import com.melly.authjwt.common.enums.ErrorType;
import com.melly.authjwt.common.exception.CustomException;
import com.melly.authjwt.common.util.CookieUtil;
import com.melly.authjwt.domain.entity.UserAuthProviderEntity;
import com.melly.authjwt.domain.entity.UserEntity;
import com.melly.authjwt.domain.repository.UserAuthProviderRepository;
import com.melly.authjwt.dto.response.OAuth2LoginResponseDto;
import com.melly.authjwt.dto.response.RefreshTokenDto;
import com.melly.authjwt.jwt.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OAuth2ServiceImpl implements OAuth2Service {

    private final UserAuthProviderRepository userAuthProviderRepository;
    private final JwtUtil jwtUtil;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CookieUtil cookieUtil;

    @Override
    public OAuth2LoginResponseDto loginWithOAuth(PrincipalDetails principal, HttpServletRequest request, HttpServletResponse response, String registrationId) {
        UserEntity user = principal.getUserEntity();
        UserAuthProviderEntity authProvider = userAuthProviderRepository
                .findByUserIdAndProviderFetchJoin(user.getUserId(), registrationId)
                .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND));

        // JWT 발급
        String tokenId = UUID.randomUUID().toString();

        String accessToken = jwtUtil.createJwt("AccessToken", user.getUsername(), user.getRole().name(), tokenId, 600000L);
        String refreshToken = jwtUtil.createJwt("RefreshToken", user.getUsername(), user.getRole().name(), tokenId, 86400000L);


        RefreshTokenDto refreshTokenDto = new RefreshTokenDto(tokenId, user.getUsername(), user.getRole().name(), LocalDateTime.now(), LocalDateTime.now().plus(Duration.ofMillis(86400000L)));
        redisTemplate.opsForValue().set("RefreshToken:" + user.getUsername() + ":" + tokenId, refreshTokenDto, Duration.ofDays(1));

        // 쿠키 생성
        Cookie refreshCookie = cookieUtil.createCookie("RefreshToken", refreshToken);
        response.addCookie(refreshCookie);

        return OAuth2LoginResponseDto.builder()
                .username(user.getUsername())
                .accessToken(accessToken )
                .refreshToken(refreshToken)
                .role(user.getRole().name())
                .message("소셜 로그인 성공")
                .success(true)
                .provider(authProvider.getProvider())
                .providerId(authProvider.getProviderId())
                .build();
    }
}
