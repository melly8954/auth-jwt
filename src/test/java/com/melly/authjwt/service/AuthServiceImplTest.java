package com.melly.authjwt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melly.authjwt.common.auth.PrincipalDetails;
import com.melly.authjwt.common.enums.ErrorType;
import com.melly.authjwt.common.exception.CustomException;
import com.melly.authjwt.common.util.CookieUtil;
import com.melly.authjwt.domain.entity.UserEntity;
import com.melly.authjwt.domain.enums.UserRole;
import com.melly.authjwt.dto.request.LoginRequestDto;
import com.melly.authjwt.dto.response.LoginResponseDto;
import com.melly.authjwt.dto.response.ReIssueTokenDto;
import com.melly.authjwt.dto.response.RefreshTokenDto;
import com.melly.authjwt.jwt.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.Authentication;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl 단위 테스트")
public class AuthServiceImplTest {
    @Mock private JwtUtil jwtUtil;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RedisTemplate<String, Object> redisTemplate;
    @Mock private ValueOperations<String, Object> valueOperations;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private Authentication authentication;
    @Mock private PrincipalDetails principalDetails;
    @Mock private UserEntity userEntity;
    @Mock private CookieUtil cookieUtil;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks AuthServiceImpl authServiceImpl;

    @Nested
    @DisplayName("login() 메서드 테스트")
    class login {
        @Test
        @DisplayName("성공 - 로그인 성공")
        void loginSuccess() {

            // given
            LoginRequestDto dto = new LoginRequestDto("testuser", "password");
            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(principalDetails);
            when(principalDetails.getUserEntity()).thenReturn(userEntity);
            when(userEntity.getUsername()).thenReturn("testuser");
            when(userEntity.getRole()).thenReturn(UserRole.USER);
            when(jwtUtil.createJwt(eq("AccessToken"), anyString(), anyString(), anyString(), anyLong()))
                    .thenReturn("access-token");
            when(jwtUtil.createJwt(eq("RefreshToken"), anyString(), anyString(), anyString(), anyLong()))
                    .thenReturn("refresh-token");
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            doNothing().when(valueOperations).set(anyString(), any(), any(Duration.class));

            // when
            LoginResponseDto result = authServiceImpl.login(dto, response);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
            assertThat(result.getAccessToken()).isEqualTo("access-token");
            assertThat(result.getRefreshToken()).isEqualTo("refresh-token");
            assertThat(result.isSuccess()).isTrue();

            // 쿠키가 response에 추가되었는지 확인
            verify(response, times(1)).addCookie(any());
            verify(valueOperations).set(anyString(), any(), any(Duration.class));
        }

        @Test
        @DisplayName("예외 - 비밀번호 불일치")
        void loginBadCredentials() {
            // given
            LoginRequestDto dto = new LoginRequestDto("testuser", "wrongpassword");
            when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("BAD_CREDENTIALS"));

            // when & then
            assertThatThrownBy(() -> authServiceImpl.login(dto, response))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorType")
                    .isEqualTo(ErrorType.BAD_CREDENTIALS);
        }

        @Test
        @DisplayName("예외 - 사용자 탈퇴")
        void loginUserDeleted() {
            // given
            LoginRequestDto dto = new LoginRequestDto("testuser", "password");
            DisabledException disabledException = new DisabledException("USER_DELETED");
            when(authenticationManager.authenticate(any())).thenThrow(disabledException);

            assertThatThrownBy(() -> authServiceImpl.login(dto, response))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorType")
                    .isEqualTo(ErrorType.USER_DELETED);
        }

        @Test
        @DisplayName("예외 - 사용자 비활성화")
        void loginUserInactive() {
            // given
            LoginRequestDto dto = new LoginRequestDto("testuser", "password");
            DisabledException disabledException = new DisabledException("Some other message");
            when(authenticationManager.authenticate(any())).thenThrow(disabledException);

            assertThatThrownBy(() -> authServiceImpl.login(dto, response))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorType")
                    .isEqualTo(ErrorType.USER_INACTIVE);
        }

        @Test
        @DisplayName("예외 - Redis 연결 실패")
        void loginRedisFailure() {
            // given
            LoginRequestDto dto = new LoginRequestDto("testuser", "password");
            when(authenticationManager.authenticate(any())).thenReturn(authentication);
            when(authentication.getPrincipal()).thenReturn(principalDetails);
            when(principalDetails.getUserEntity()).thenReturn(userEntity);
            when(userEntity.getUsername()).thenReturn("testuser");
            when(userEntity.getRole()).thenReturn(UserRole.USER);
            when(jwtUtil.createJwt(anyString(), anyString(), anyString(), anyString(), anyLong()))
                    .thenReturn("token");
            doThrow(new RedisConnectionFailureException("Redis down")).when(redisTemplate).opsForValue();

            assertThatThrownBy(() -> authServiceImpl.login(dto, response))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorType")
                    .isEqualTo(ErrorType.REDIS_CONNECTION_ERROR);
        }
    }

    @Nested
    @DisplayName("reissueToken() 메서드 테스트")
    class reissueToken {
            @Test
            @DisplayName("성공 - 토큰 재발급")
            void testReissueTokenSuccess() {
                String oldToken = "oldRefreshToken";
                String username = "user1";
                String tokenId = "tokenId1";
                RefreshTokenDto oldDto = new RefreshTokenDto(tokenId, username, "ROLE_USER", LocalDateTime.now(), LocalDateTime.now().plusDays(1));

                when(cookieUtil.getValue(request)).thenReturn(oldToken);
                when(jwtUtil.isExpired(oldToken)).thenReturn(false);
                when(jwtUtil.getCategory(oldToken)).thenReturn("RefreshToken");
                when(jwtUtil.getUsername(oldToken)).thenReturn(username);
                when(jwtUtil.getTokenId(oldToken)).thenReturn(tokenId);

                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
                when(valueOperations.get(anyString())).thenReturn(oldDto);
                when(objectMapper.convertValue(any(), eq(RefreshTokenDto.class))).thenReturn(oldDto);
                doNothing().when(valueOperations).set(anyString(), any(), any(Duration.class));

                when(jwtUtil.createJwt(eq("AccessToken"), eq(username), any(), any(), anyLong())).thenReturn("newAccessToken");
                when(jwtUtil.createJwt(eq("RefreshToken"), eq(username), any(), any(), anyLong())).thenReturn("newRefreshToken");

                when(cookieUtil.getValue(request)).thenReturn("oldRefreshToken");
                when(cookieUtil.createCookie("RefreshToken", "newRefreshToken"))
                        .thenReturn(new Cookie("RefreshToken", "newRefreshToken"));

                ReIssueTokenDto result = authServiceImpl.reissueToken(request, response);

                assertThat(result).isNotNull();
                assertThat(result.getNewAccessToken()).isEqualTo("newAccessToken");
                assertThat(result.getNewRefreshToken()).isEqualTo("newRefreshToken");
            }

        @Test
        @DisplayName("예외 - 존재하지 않는 Refresh Token")
        void testRefreshTokenMissing() {
            when(cookieUtil.getValue(request)).thenReturn(null);

            assertThatThrownBy(() -> authServiceImpl.reissueToken(request, response))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorType")
                    .isEqualTo(ErrorType.REFRESH_TOKEN_NOT_FOUND);
        }

        @Test
        @DisplayName("예외 - 만료된 Refresh Token")
        void testExpiredRefreshToken() {
            when(cookieUtil.getValue(request)).thenReturn("dummyRefreshToken");
            when(jwtUtil.isExpired("dummyRefreshToken")).thenReturn(true);

            assertThatThrownBy(() -> authServiceImpl.reissueToken(request, response))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorType")
                    .isEqualTo(ErrorType.EXPIRED_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("예외 - 다른 카테고리의 Refresh Token")
        void testInvalidCategory() {
            when(cookieUtil.getValue(request)).thenReturn("dummyRefreshToken");
            when(jwtUtil.isExpired("dummyRefreshToken")).thenReturn(false);
            when(jwtUtil.getCategory("dummyRefreshToken")).thenReturn("AccessToken");

            assertThatThrownBy(() -> authServiceImpl.reissueToken(request, response))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorType")
                    .isEqualTo(ErrorType.INVALID_REFRESH_TOKEN);
        }

        @Test
        @DisplayName("예외 - Redis에 존재하지 않는 Refresh Token")
        void testTokenNotFoundInRedis() {
            when(cookieUtil.getValue(request)).thenReturn("dummyRefreshToken");
            when(jwtUtil.isExpired("dummyRefreshToken")).thenReturn(false);
            when(jwtUtil.getCategory("dummyRefreshToken")).thenReturn("RefreshToken");
            when(jwtUtil.getUsername("dummyRefreshToken")).thenReturn("user1");
            when(jwtUtil.getTokenId("dummyRefreshToken")).thenReturn("tokenId1");

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(anyString())).thenReturn(null);

            assertThatThrownBy(() -> authServiceImpl.reissueToken(request, response))
                    .isInstanceOf(CustomException.class)
                    .extracting("errorType")
                    .isEqualTo(ErrorType.REFRESH_TOKEN_NOT_FOUND_IN_REDIS);
        }
    }

    @Nested
    @DisplayName("logout() 메서드 테스트")
    class logout {
        @Test
        @DisplayName("성공 - 로그아웃 성공")
        void logout_success() {
            String accessToken = "access-token";
            String refreshToken = "refresh-token";

            // Header mock
            when(request.getHeader("Authorization")).thenReturn("Bearer " + accessToken);

            // Redis TTL mock
            when(jwtUtil.getExpiration(accessToken)).thenReturn(1000L); // 1초

            // Refresh token mock
            when(cookieUtil.getValue(request)).thenReturn(refreshToken);
            when(jwtUtil.getUsername(refreshToken)).thenReturn("user1");
            when(jwtUtil.getTokenId(refreshToken)).thenReturn("token-id");

            // Redis mock
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            doNothing().when(valueOperations).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
            when(redisTemplate.delete(anyString())).thenReturn(true);

            // Response cookie mock
            doNothing().when(response).addCookie(any(Cookie.class));

            authServiceImpl.logout(request, response);

            // 검증
            verify(valueOperations).set(
                    eq("BLACKLIST_" + accessToken),
                    eq("logout"),
                    eq(1000L),
                    eq(TimeUnit.MILLISECONDS)
            );
            verify(redisTemplate).delete("RefreshToken:user1:token-id");
            verify(response).addCookie(any(Cookie.class));
        }
    }
}
