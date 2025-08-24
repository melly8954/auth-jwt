package com.melly.authjwt.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorType {
    // 공통
    BAD_REQUEST("bad_request", "잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("unauthorized", "인증되지 않은 사용자입니다.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("forbidden", "권한이 없습니다.", HttpStatus.FORBIDDEN),
    NOT_FOUND("not_found", "리소스를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
    CONFLICT("conflict", "요청이 현재 상태와 충돌합니다.", HttpStatus.CONFLICT),
    INTERNAL_ERROR("internal_error", "서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),

    // 회원가입
    DUPLICATE_USERNAME("duplicate_username", "이미 사용 중인 아이디입니다.", HttpStatus.CONFLICT),
    DUPLICATE_EMAIL("duplicate_email", "이미 사용 중인 이메일입니다.", HttpStatus.CONFLICT),
    PASSWORD_MISMATCH("password_mismatch", "비밀번호와 비밀번호 확인이 일치하지 않습니다.", HttpStatus.BAD_REQUEST),

    BAD_CREDENTIALS("bad_credentials", "비밀번호가 일치하지 않습니다.", HttpStatus.UNAUTHORIZED),
    USER_INACTIVE("user_inactive", "비활성화된 계정입니다.", HttpStatus.UNAUTHORIZED),
    USER_NOT_FOUND("user_not_found", "해당 정보로 등록된 사용자가 없습니다.", HttpStatus.NOT_FOUND),
    USER_DELETED("user_deleted", "탈퇴된 계정입니다.", HttpStatus.UNAUTHORIZED),

    // Redis 관련 에러
    REDIS_CONNECTION_ERROR("redis_connection_error","Redis 연결 실패", HttpStatus.INTERNAL_SERVER_ERROR),   // 연결 실패
    REDIS_COMMAND_ERROR("redis_command_error","Redis 명령 실행 실패", HttpStatus.INTERNAL_SERVER_ERROR); // 명령/접속 문제


    private final String errorCode;      // errorCode
    private final String message;       // 사용자 메시지
    private final HttpStatus status;        // HTTP 상태
}
