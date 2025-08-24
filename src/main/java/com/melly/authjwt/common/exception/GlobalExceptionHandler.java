package com.melly.authjwt.common.exception;

import com.melly.authjwt.common.controller.ResponseController;
import com.melly.authjwt.common.dto.ResponseDto;
import com.melly.authjwt.common.enums.ErrorType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler implements ResponseController {
    // 커스텀 비즈니스 예외 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ResponseDto<Void>> handleCustomException(CustomException e) {
        ErrorType errorType = e.getErrorType();
        log.error("비즈니스 로직 예외 발생 - Code: {}, Message: {}", errorType.getErrorCode(), errorType.getMessage());

        return makeResponseEntity(
                errorType.getStatus(),
                errorType.getErrorCode(),
                errorType.getMessage(),
                null
        );
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDto<Void>> handleException(Exception e) {
        log.error("서버 내부 예외 발생 - Message: {}", e.getMessage());
        return makeResponseEntity(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorType.INTERNAL_ERROR.getErrorCode(),
                "서버 내부 오류가 발생했습니다.",
                null);
    }
}
