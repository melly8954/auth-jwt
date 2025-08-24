package com.melly.authjwt.common.exception;

import com.melly.authjwt.common.enums.ErrorType;
import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
    private final ErrorType errorType;

    public CustomException(ErrorType errorType) {
        super(errorType.getMessage());
        this.errorType = errorType;
    }
}
