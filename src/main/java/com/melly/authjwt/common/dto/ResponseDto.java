package com.melly.authjwt.common.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ResponseDto<T> {
    private int code;
    private String errorCode;
    private String message;
    private T data;
}
