package com.melly.authjwt.service;

import com.melly.authjwt.common.auth.PrincipalDetails;
import com.melly.authjwt.common.enums.ErrorType;
import com.melly.authjwt.common.exception.CustomException;
import com.melly.authjwt.domain.entity.UserEntity;
import com.melly.authjwt.dto.request.LoginRequestDto;
import com.melly.authjwt.dto.response.LoginResponseDto;
import com.melly.authjwt.jwt.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;

    @Override
    public LoginResponseDto login(LoginRequestDto dto) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword())
            );

            UserEntity user = ((PrincipalDetails) authentication.getPrincipal()).getUserEntity();
            String accessToken = jwtUtil.createJwt("AccessToken", user.getUsername(), user.getRole().name(), 600000L);

            return LoginResponseDto.builder()
                    .username(user.getUsername())
                    .token(accessToken)
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
        }
    }
}
