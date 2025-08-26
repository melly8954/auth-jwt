# Auth-Jwt

Spring Security 기반 **JWT 인증** 학습/실습 프로젝트입니다.  
Access/Refresh Token 발급, Redis에 Refresh Token 저장, 토큰 재발급(로테이션), 로그아웃 구현을 직접 구현/분석했습니다.  

자세한 인증 흐름 및 코드 해설은 [Notion 문서](https://northern-mongoose-47b.notion.site/Auth-Jwt-25bd351413c0806e9a18e34bfd02c911) 에 정리되어 있습니다.

---

**Backend & Runtime**
- Java 17
- Spring Boot 3.4.9
- Spring Security
- Spring Data JPA
- MySQL 8.4
- Redis
- Gradle

**Development Tools & Testing**
- Lombok
- JUnit 5
- AssertJ
- Spring Security Test
- p6spy (SQL 로그 확인용)
---

**주요 기능**
- 회원가입
- 로그인
  - JWT 발급
    - Access Token (짧은 유효기간)
    - Refresh Token (상대적으로 긴 유효기간)
  - Redis에 Refresh Token 저장
  - 클라이언트 쿠키에 Refresh Token 저장 (HttpOnly 옵션)
  - 인증 요청 시 JWT를 통한 검증 (JwtFilter)
- 인증 토큰 재발급 (Access Token 만료 시)
  - Redis에 저장된 Refresh Token 검증
  - 신규 Access/Refresh Token 발급 (Refresh Token 로테이션)
  - 기존 Refresh Token 삭제 후 신규 Refresh Token Redis에 저장
  - 클라이언트 쿠키에 신규 Refresh Token 저장
- 로그아웃
  - Access Token 블랙리스트 등록
  - Refresh Token 무효화
  - 클라이언트 쿠키에서 Refresh Token 제거 
- OAuth2 로그인: Google 소셜 계정 연동 (환경변수로 클라이언트 ID/Secret 관리)
- 인증/인가 실패 예외 처리 (401, 403)
- 테스트 커버리지
  - 회원가입, 로그인, 로그아웃, 토큰 갱신, 소셜 로그인<br><br>
---


**데이터베이스 초기 설정**
```sql
create database auth_jwt character set utf8mb4 collate utf8mb4_general_ci;
create user `psw_auth`@`%` identified by 'auth1234!';
grant all privileges on auth_jwt.* to `psw_auth`@`%` with grant option;
flush privileges;
```


#### 사용자 테이블
```sql
CREATE TABLE `user_tbl` (
  `user_id` bigint NOT NULL AUTO_INCREMENT,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL,
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'USER',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'ACTIVE',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`),
  UNIQUE KEY `email` (`email`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

#### 인증 제공자 테이블 (소셜 로그인용)
```sql
CREATE TABLE `user_auth_provider_tbl` (
  `auth_provider_id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `provider` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `provider_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`auth_provider_id`),
  UNIQUE KEY `uk_provider_user` (`provider`,`provider_id`),
  KEY `user_id` (`user_id`),
  CONSTRAINT `user_auth_provider_tbl_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_tbl` (`user_id`) ON DELETE CASCADE
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
```

| 메서드  | 엔드포인트                      | 설명                      |
| ---- | -------------------------- | ----------------------- |
| POST | /api/v1/users        | 회원가입                    |
| POST | /api/v1/auth/login         | 로그인 (JWT 발급)            |
| POST | /api/v1/auth/logout        | 로그아웃 (Refresh Token 삭제) |
| POST | /api/v1/auth/reissue | 토큰 갱신                   |
| GET  | /api/v1/users/test            | 인증된 사용자 정보 조회           |
| GET  | /api/v1/admins/test | 관리자 권한을 가진 인증된 사용자 정보 조회 |
