package com.melly.authjwt.common.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class CookieUtil {
    // 쿠키 생성 메서드
    public Cookie createCookie(String key, String value) {
        Cookie cookie = new Cookie(key, value);
        cookie.setMaxAge(24*60*60);
        cookie.setPath("/");        // 쿠키가 사이트의 모든 경로에서 유효하도록 설정
        cookie.setHttpOnly(true);
        cookie.setSecure(false);        // 로컬 개발 환경에서는 false로 설정 (HTTPS 연결이 필요 없는 경우)
        return cookie;
    }


    // 쿠키 조회
    public String getValue(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        String refreshToken = null;
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("RefreshToken".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                }
            }
        }
        return refreshToken;
    }
}
