package com.melly.authjwt.common.controller;

import com.melly.authjwt.common.dto.ResponseDto;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public interface ResponseController {
    default <T> ResponseEntity<ResponseDto<T>> makeResponseEntity(HttpStatus httpStatus, String errorCode, String message, T data) {
        ResponseDto<T> responseDto = ResponseDto.<T>builder()
                .code(httpStatus.value())
                .errorCode(errorCode)
                .message(message)
                .data(data)
                .build();
        return ResponseEntity.status(httpStatus).body(responseDto);
    }
}
