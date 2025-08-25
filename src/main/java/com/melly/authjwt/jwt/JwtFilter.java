package com.melly.authjwt.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melly.authjwt.common.auth.PrincipalDetails;
import com.melly.authjwt.common.dto.ResponseDto;
import com.melly.authjwt.common.enums.ErrorType;
import com.melly.authjwt.common.exception.CustomException;
import com.melly.authjwt.domain.entity.UserEntity;
import com.melly.authjwt.domain.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.io.PrintWriter;

// Spring Security 필터 체인에서 Jwt 인증을 수행하는 핵심 필터입니다.
// 요청이 들어올 때마다 실행되며, 유효한 Access Token 이 포함되어 있는지 확인하고, 있다면 이를 기반으로 SecurityContext 에 인증 정보를 등록
@Slf4j
public class JwtFilter extends OncePerRequestFilter {
    // OncePerRequestFilter 는 Spring Framework 에서 제공하는 추상 클래스로, 하나의 요청(request)당 딱 한 번만 실행되는 필터
    // JWT 검증 로직은 인증이 필요한 요청이 들어올 때마다 확실히 한 번만 실행되도록 보장
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public JwtFilter(JwtUtil jwtUtil, UserRepository userRepository, RedisTemplate<String, Object> redisTemplate) {
        this.jwtUtil = jwtUtil;
        this.userRepository = userRepository;
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        // 헤더에서 access 키에 담긴 토큰을 꺼냄
        String header = request.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String accessToken = header.substring(7); // "Bearer " 이후의 토큰만 추출

        // 토큰 만료 여부 확인
        if (jwtUtil.isExpired(accessToken)) {
            log.error("Expired JWT token");

            sendErrorResponse(
                    response,
                    ErrorType.EXPIRED_ACCESS_TOKEN.getStatus().value(),
                    ErrorType.EXPIRED_ACCESS_TOKEN.getErrorCode(),
                    ErrorType.EXPIRED_ACCESS_TOKEN.getMessage()
            );
            return;
        }

        // 토큰의 종류가 access 인지 검증 (발급시 페이로드에 명시)
        String category = jwtUtil.getCategory(accessToken);

        if (!category.equals("AccessToken")) {
            log.error("Invalid JWT token");

            sendErrorResponse(
                    response,
                    ErrorType.INVALID_ACCESS_TOKEN.getStatus().value(),
                    ErrorType.INVALID_ACCESS_TOKEN.getErrorCode(),
                    ErrorType.INVALID_ACCESS_TOKEN.getMessage()
            );
            return;
        }

        // 블랙리스트 체크
        Boolean isBlacklisted = redisTemplate.hasKey("BLACKLIST_" + accessToken);
        if (Boolean.TRUE.equals(isBlacklisted)) {
            log.error("Blacklisted JWT token");

            sendErrorResponse(
                    response,
                    ErrorType.TOKEN_BLACKLISTED.getStatus().value(),
                    ErrorType.TOKEN_BLACKLISTED.getErrorCode(),
                    ErrorType.TOKEN_BLACKLISTED.getMessage()
            );
            return;
        }

        // username 값 획득
        String username = jwtUtil.getUsername(accessToken);

        UserEntity user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomException(ErrorType.USER_NOT_FOUND));

        PrincipalDetails principalDetails = new PrincipalDetails(user);

        Authentication authToken = new UsernamePasswordAuthenticationToken(principalDetails, null, principalDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }

    // Spring Security Filter 내부에서는 일반적으로 @ControllerAdvice 기반 전역 예외처리(@ExceptionHandler)가 동작하지 않기에 JSON Response 처리
    private void sendErrorResponse(HttpServletResponse response, int status, String errorCode, String message) throws IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(status);

        ResponseDto<Object> responseDto = ResponseDto.builder()
                .code(status)
                .errorCode(errorCode)
                .message(message)
                .data(null)
                .build();

        ObjectMapper objectMapper = new ObjectMapper();
        String json = objectMapper.writeValueAsString(responseDto);

        PrintWriter writer = response.getWriter();
        writer.write(json);
        writer.flush();
    }
}
