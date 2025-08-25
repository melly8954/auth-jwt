package com.melly.authjwt.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melly.authjwt.common.auth.*;
import com.melly.authjwt.domain.repository.UserRepository;
import com.melly.authjwt.dto.response.OAuth2LoginResponseDto;
import com.melly.authjwt.jwt.JwtFilter;
import com.melly.authjwt.jwt.JwtUtil;
import com.melly.authjwt.service.OAuth2Service;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;
    private final JwtUtil jwtUtil;
    private final PrincipalOAuth2UserService principalOAuth2UserService;
    private final OAuth2Service oAuth2Service;
    private final RedisTemplate<String, Object> redisTemplate;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/api/v1/users",
                                "/api/v1/auth/login",
                                "/api/v1/auth/logout",
                                "/api/v1/auth/reissue")
                        .permitAll()
                        .requestMatchers(
                                "/api/v1/admins/**")
                        .hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(customAuthenticationEntryPoint)
                        .accessDeniedHandler(customAccessDeniedHandler)
                )
                // JwtFilter 를 UsernamePasswordAuthenticationFilter 앞에 추가
                .addFilterBefore(new JwtFilter(jwtUtil,userRepository,redisTemplate), UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo.userService(principalOAuth2UserService))
                        .successHandler((request, response, authentication) -> {
                            OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
                            String registrationId = token.getAuthorizedClientRegistrationId();
                            PrincipalDetails principalDetails = (PrincipalDetails) authentication.getPrincipal();

                            OAuth2LoginResponseDto dto = oAuth2Service.loginWithOAuth(principalDetails, request, response, registrationId);

                            response.setContentType("application/json");
                            response.setCharacterEncoding("UTF-8");
                            response.getWriter().write(new ObjectMapper().writeValueAsString(dto));
                        })
                );
        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider customAuthenticationProvider() {
        return new CustomAuthenticationProvider(userRepository, passwordEncoder);
    }
}
