package com.melly.authjwt.service;

import com.melly.authjwt.common.auth.PrincipalDetails;
import com.melly.authjwt.common.enums.ErrorType;
import com.melly.authjwt.common.exception.CustomException;
import com.melly.authjwt.domain.entity.UserAuthProviderEntity;
import com.melly.authjwt.domain.entity.UserEntity;
import com.melly.authjwt.domain.repository.UserAuthProviderRepository;
import com.melly.authjwt.dto.response.OAuth2LoginResponseDto;
import com.melly.authjwt.dto.response.RefreshTokenDto;
import com.melly.authjwt.jwt.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
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

    @Override
    public OAuth2LoginResponseDto loginWithOAuth(PrincipalDetails principal, HttpServletRequest request, String registrationId) {
        UserEntity user = principal.getUserEntity();
        UserAuthProviderEntity authProvider = userAuthProviderRepository
                .findByUserIdAndProviderFetchJoin(user.getUserId(), registrationId)
                .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND));

        // JWT 발급
        String accessToken = jwtUtil.createJwt("AccessToken", user.getUsername(), user.getRole().name(), 600000L);
        String refreshToken = jwtUtil.createJwt("RefreshToken", user.getUsername(), user.getRole().name(), 86400000L);

        String tokenId = UUID.randomUUID().toString();
        RefreshTokenDto refreshTokenDto = new RefreshTokenDto(tokenId, user.getUserId(), LocalDateTime.now(), LocalDateTime.now().plus(Duration.ofMillis(86400000L)));
        redisTemplate.opsForValue().set("refresh:" + user.getUserId() + ":" + tokenId, refreshTokenDto, Duration.ofDays(1));

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
