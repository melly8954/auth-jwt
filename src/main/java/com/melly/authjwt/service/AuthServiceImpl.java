package com.melly.authjwt.service;

import com.melly.authjwt.common.auth.PrincipalDetails;
import com.melly.authjwt.common.enums.ErrorType;
import com.melly.authjwt.common.exception.CustomException;
import com.melly.authjwt.domain.entity.UserEntity;
import com.melly.authjwt.dto.request.LoginRequestDto;
import com.melly.authjwt.dto.response.LoginResponseDto;
import com.melly.authjwt.dto.response.RefreshTokenDto;
import com.melly.authjwt.jwt.JwtUtil;
import io.lettuce.core.RedisCommandTimeoutException;
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

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public LoginResponseDto login(LoginRequestDto dto) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
            );

            UserEntity user = ((PrincipalDetails) authentication.getPrincipal()).getUserEntity();
            String accessToken = jwtUtil.createJwt("AccessToken", user.getUsername(), user.getRole().name(), 600000L);
            String refreshToken = jwtUtil.createJwt("RefreshToken", user.getUsername(), user.getRole().name(), 86400000L);

            String tokenId = UUID.randomUUID().toString();
            RefreshTokenDto refreshTokenDto = new RefreshTokenDto(tokenId, user.getUserId(), LocalDateTime.now(), LocalDateTime.now().plus(Duration.ofMillis(86400000L)));
            redisTemplate.opsForValue().set("refresh:" + user.getUserId() + ":" + tokenId, refreshTokenDto, Duration.ofDays(1));

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
}
