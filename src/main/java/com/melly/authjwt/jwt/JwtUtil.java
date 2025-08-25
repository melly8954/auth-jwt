package com.melly.authjwt.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    private SecretKey secretKey;

    public JwtUtil(@Value("${spring.jwt.secret}")String secret) {
        // String 타입의 secret 을 객체변수(secretKey) 로 암호화
        this.secretKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
    }

    public String getCategory(String token){
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("category", String.class);
    }

    public String getUsername(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("username", String.class);
    }

    public String getRole(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("role", String.class);
    }

    public String getTokenId(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload().get("tokenId", String.class);
    }

    public Boolean isExpired(String token) {
        try {
            // parseSignedClaims 시 이미 만료 검사 포함
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return false; // 예외 없으면 만료되지 않음
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return true;  // 만료됨
        }
    }

    // Jwt 생성
    public String createJwt(String category, String username, String role, String tokenId,Long expiredMs){
        return Jwts.builder()
                .claim("category",category)
                .claim("username",username)
                .claim("role",role)
                .claim("tokenId", tokenId)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiredMs))
                .signWith(secretKey)
                .compact();
    }

    public long getExpiration(String token) {
        try {
            // 토큰 검증 + 파싱
            Jwt<?, Claims> jwt = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);

            Claims claims = jwt.getPayload();
            Date exp = claims.getExpiration(); // "exp"를 Date로 가져옴
            return exp.getTime() - System.currentTimeMillis();

        } catch (ExpiredJwtException e) {
            // 이미 만료된 경우, 남은 시간은 0
            return 0;
        }
    }
}
