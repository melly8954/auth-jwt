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
  - Redis에 Refresh Token 저장 (키: `RefreshToken:{username}:{tokenId}`)
  - 클라이언트 쿠키에 Refresh Token 저장 (HttpOnly 옵션)
  - 인증 요청 시 JWT를 통한 검증 (JwtFilter)
- 인증 토큰 재발급 (Access Token 만료 시)
  - Redis에 저장된 Refresh Token 검증
  - 신규 Access/Refresh Token 발급 (Refresh Token 로테이션)
  - 기존 Refresh Token 삭제 후 신규 Refresh Token Redis에 저장
  - 클라이언트 쿠키에 신규 Refresh Token 저장
- 로그아웃
  - Redis에 Access Token 블랙리스트 등록 (키 : `BLACKLIST_<Access Token>`)
  - Refresh Token 무효화
  - 클라이언트 쿠키에서 Refresh Token 제거 
- OAuth2 로그인: Google 소셜 계정 연동 (환경변수로 클라이언트 ID/Secret 관리)
- 인증/인가 실패 예외 처리 (401, 403)
- 테스트 커버리지
  - 회원가입, 로그인, 로그아웃, 토큰 갱신, 소셜 로그인<br><br>
---

## 🚀 프로젝트 실행 방법

**1️⃣ 환경 변수 설정**
프로젝트 루트에 `.env` 파일 생성 후, 필요한 환경 변수를 설정합니다.
[.env 파일 템플릿]()

**2️⃣ Gradle 빌드**
```bash
./gradlew build
```

**3️⃣ DB 및 Redis 실행 (Docker Compose)**
```bash
docker-compose up -d
```

MySQL과 Redis가 백그라운드에서 실행됩니다.

**4️⃣ Docker 이미지 빌드**
```bash
docker build -t auth-jwt:latest .
```

**5️⃣ 애플리케이션 실행**
```bash
docker run -p 8080:8080 --network auth-jwt_default --env-file .env -e "SPRING_PROFILES_ACTIVE=docker" --name auth-jwt-app auth-jwt:latest
```
<hr>

#### API 명세
| Method  | Endpoint                      | 설명                      |
| ---- | -------------------------- | ----------------------- |
| POST | /api/v1/users        | 회원가입                    |
| POST | /api/v1/auth/login         | 로그인 (JWT 발급)            |
| POST | /api/v1/auth/logout        | 로그아웃 (Refresh Token 삭제) |
| POST | /api/v1/auth/reissue | 토큰 갱신                   |
| GET  | /api/v1/users/test            | 인증된 사용자 정보 조회           |
| GET  | /api/v1/admins/test | 관리자 권한을 가진 인증된 사용자 정보 조회 |
