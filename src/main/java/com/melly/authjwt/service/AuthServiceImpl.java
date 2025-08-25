package com.melly.authjwt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melly.authjwt.common.auth.PrincipalDetails;
import com.melly.authjwt.common.enums.ErrorType;
import com.melly.authjwt.common.exception.CustomException;
import com.melly.authjwt.common.util.CookieUtil;
import com.melly.authjwt.domain.entity.UserEntity;
import com.melly.authjwt.dto.request.LoginRequestDto;
import com.melly.authjwt.dto.response.LoginResponseDto;
import com.melly.authjwt.dto.response.ReIssueTokenDto;
import com.melly.authjwt.dto.response.RefreshTokenDto;
import com.melly.authjwt.jwt.JwtUtil;
import io.lettuce.core.RedisCommandTimeoutException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.RedisSystemException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    
    @Override
    public LoginResponseDto login(LoginRequestDto dto, HttpServletResponse response) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
            );

            UserEntity user = ((PrincipalDetails) authentication.getPrincipal()).getUserEntity();
            String tokenId = UUID.randomUUID().toString();

            String accessToken = jwtUtil.createJwt("AccessToken", user.getUsername(), user.getRole().name(), tokenId,600000L);
            String refreshToken = jwtUtil.createJwt("RefreshToken", user.getUsername(), user.getRole().name(), tokenId, 86400000L);


            RefreshTokenDto refreshTokenDto = new RefreshTokenDto(tokenId, user.getUsername(), user.getRole().name(), LocalDateTime.now(), LocalDateTime.now().plus(Duration.ofMillis(86400000L)));
            redisTemplate.opsForValue().set("RefreshToken:" + user.getUsername() + ":" + tokenId, refreshTokenDto, Duration.ofDays(1));

            // 쿠키 생성
            Cookie refreshCookie = CookieUtil.createCookie("RefreshToken", refreshToken);
            response.addCookie(refreshCookie);

            return LoginResponseDto.builder()
                    .username(user.getUsername())
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .role(user.getRole().name())
                    .message("로그인 성공")
                    .success(true)
                    .build();
        } catch (BadCredentialsException e) {
            throw new CustomException(ErrorType.BAD_CREDENTIALS);
        } catch (DisabledException e) {
            if ("USER_DELETED".equals(e.getMessage())) {
                throw new CustomException(ErrorType.USER_DELETED);
            }
            throw new CustomException(ErrorType.USER_INACTIVE);
        } catch (AuthenticationException e) {
            throw new CustomException(ErrorType.INTERNAL_ERROR);
        } catch (RedisConnectionFailureException | RedisCommandTimeoutException e) {
            throw new CustomException(ErrorType.REDIS_CONNECTION_ERROR);
        } catch (RedisSystemException e) {
            throw new CustomException(ErrorType.REDIS_COMMAND_ERROR);
        }
    }

    @Override
    public ReIssueTokenDto reissueToken(HttpServletRequest request, HttpServletResponse response) {
        // 쿠키에서 refresh 토큰 추출
        String refreshToken = CookieUtil.getValue(request);
        if (refreshToken == null) {
            throw new CustomException(ErrorType.REFRESH_TOKEN_NOT_FOUND);
        }

        // 토큰 만료 확인
        if (jwtUtil.isExpired(refreshToken)) {
            throw new CustomException(ErrorType.EXPIRED_REFRESH_TOKEN);
        }

        // 카테고리 확인
        if (!"RefreshToken".equals(jwtUtil.getCategory(refreshToken))) {
            throw new CustomException(ErrorType.INVALID_REFRESH_TOKEN);
        }

        String username = jwtUtil.getUsername(refreshToken);
        String tokenId = jwtUtil.getTokenId(refreshToken);

        String key = "RefreshToken:" + username + ":" + tokenId;
        Object redisValue = redisTemplate.opsForValue().get(key);

        if (redisValue == null) {
            throw new CustomException(ErrorType.REFRESH_TOKEN_NOT_FOUND_IN_REDIS);
        }

        // Object -> DTO 변환
        RefreshTokenDto refreshTokenDto = objectMapper.convertValue(redisValue, RefreshTokenDto.class);

        // 새로운 tokenId 생성
        String newTokenId = UUID.randomUUID().toString();

        // 새로운 accessToken, refreshToken 생성
        String newAccessToken = jwtUtil.createJwt("AccessToken", username, refreshTokenDto.getRole(), newTokenId, 600000L);
        String newRefreshToken = jwtUtil.createJwt("RefreshToken", username, refreshTokenDto.getRole(), newTokenId, 86400000L);

        // Redis에 새로운 refreshToken 저장
        RefreshTokenDto newRefreshTokenDto = new RefreshTokenDto(
                newTokenId, username, refreshTokenDto.getRole(),
                LocalDateTime.now(),
                LocalDateTime.now().plus(Duration.ofMillis(86400000L))
        );
        redisTemplate.opsForValue().set("RefreshToken:" + username + ":" + newTokenId, newRefreshTokenDto, Duration.ofDays(1));

        // 기존 refresh token 삭제
        redisTemplate.delete(key);

        // 쿠키에 새로운 refreshToken 저장
        Cookie refreshCookie = CookieUtil.createCookie("RefreshToken", newRefreshToken);
        response.addCookie(refreshCookie);

        return new ReIssueTokenDto(newAccessToken, newRefreshToken);
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String accessToken = request.getHeader("Authorization");
        if (accessToken != null && accessToken.startsWith("Bearer ")) {
            accessToken = accessToken.substring(7); // "Bearer " 제거
        }

        // 토큰에서 남은 만료 시간 계산
        long expiration = jwtUtil.getExpiration(accessToken);

        // Redis 블랙리스트에 저장 (TTL 설정)
        if (expiration > 0) {
            redisTemplate.opsForValue().set(
                    "BLACKLIST_" + accessToken,
                    "logout",
                    expiration,
                    TimeUnit.MILLISECONDS
            );
        }

        String refreshToken = CookieUtil.getValue(request);
        String username = jwtUtil.getUsername(refreshToken);
        String tokenId = jwtUtil.getTokenId(refreshToken);

        String key = "RefreshToken:" + username + ":" + tokenId;

        redisTemplate.delete(key);

        // 쿠키에서 refresh token 제거
        Cookie refreshCookie = new Cookie("RefreshToken", null);
        refreshCookie.setHttpOnly(true);
        refreshCookie.setPath("/");
        refreshCookie.setMaxAge(0); // 바로 만료
        response.addCookie(refreshCookie);
    }
}
