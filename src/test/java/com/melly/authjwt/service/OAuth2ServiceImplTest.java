package com.melly.authjwt.service;

import com.melly.authjwt.common.auth.PrincipalDetails;
import com.melly.authjwt.common.enums.ErrorType;
import com.melly.authjwt.common.exception.CustomException;
import com.melly.authjwt.common.util.CookieUtil;
import com.melly.authjwt.domain.entity.UserAuthProviderEntity;
import com.melly.authjwt.domain.entity.UserEntity;
import com.melly.authjwt.domain.enums.UserRole;
import com.melly.authjwt.domain.repository.UserAuthProviderRepository;
import com.melly.authjwt.dto.response.OAuth2LoginResponseDto;
import com.melly.authjwt.dto.response.RefreshTokenDto;
import com.melly.authjwt.jwt.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OAuth2ServiceImpl 단위 테스트")
public class OAuth2ServiceImplTest {
    @Mock private UserAuthProviderRepository userAuthProviderRepository;
    @Mock private JwtUtil jwtUtil;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private CookieUtil cookieUtil;
    @Mock private PrincipalDetails principal;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;

    @InjectMocks private OAuth2ServiceImpl oAuth2Service;

    private UserEntity user;
    private UserAuthProviderEntity authProvider;

    @BeforeEach
    void setup() {
        user = UserEntity.builder()
                .userId(1L)
                .username("testuser")
                .role(UserRole.USER)
                .build();

        authProvider = UserAuthProviderEntity.builder()
                .provider("google")
                .providerId("google-123")
                .build();
    }

    @Nested
    @DisplayName("loginWithOAuth() 메서드 테스트")
    class loginWithOAuth {
        @Test
        @DisplayName("성공 - OAuth2 로그인 성공")
        void login_success() {
            // principal에서 UserEntity 반환
            when(principal.getUserEntity()).thenReturn(user);

            // UserAuthProviderRepository mock
            when(userAuthProviderRepository.findByUserIdAndProviderFetchJoin(user.getUserId(), "google"))
                    .thenReturn(Optional.of(authProvider));

            // JWT mock
            String accessToken = "access-token";
            String refreshToken = "refresh-token";
            when(jwtUtil.createJwt(
                    eq("AccessToken"),
                    eq(user.getUsername()),
                    eq(user.getRole().name()),
                    anyString(),
                    eq(600000L)))
                    .thenReturn(accessToken);

            when(jwtUtil.createJwt(
                    eq("RefreshToken"),
                    eq(user.getUsername()),
                    eq(user.getRole().name()),
                    anyString(),
                    eq(86400000L)))
                    .thenReturn(refreshToken);

            // Redis opsForValue mock
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            doNothing().when(valueOperations).set(anyString(), any(), any(Duration.class));

            // Cookie mock
            Cookie cookie = new Cookie("RefreshToken", refreshToken);
            when(cookieUtil.createCookie("RefreshToken", refreshToken)).thenReturn(cookie);
            doNothing().when(response).addCookie(cookie);

            // 실제 호출
            OAuth2LoginResponseDto result = oAuth2Service.loginWithOAuth(principal, request, response, "google");

            // 검증
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
            assertThat(result.getAccessToken()).isEqualTo(accessToken);
            assertThat(result.getRefreshToken()).isEqualTo(refreshToken);
            assertThat(result.getProvider()).isEqualTo("google");
            assertThat(result.getProviderId()).isEqualTo("google-123");
            assertThat(result.isSuccess()).isTrue();

            // Redis와 쿠키 호출 검증
            verify(valueOperations).set(
                    startsWith("RefreshToken:" + user.getUsername() + ":"),
                    any(RefreshTokenDto.class),
                    eq(Duration.ofDays(1))
            );
            verify(response).addCookie(cookie);
        }

        @Test
        @DisplayName("예외 - UserAuthProvider 미존재")
        void login_userAuthProviderNotFound() {
            when(principal.getUserEntity()).thenReturn(user);
            when(userAuthProviderRepository.findByUserIdAndProviderFetchJoin(user.getUserId(), "google"))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> oAuth2Service.loginWithOAuth(principal, request, response, "google"))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorType")
                    .isEqualTo(ErrorType.USER_NOT_FOUND);
        }
    }
}
