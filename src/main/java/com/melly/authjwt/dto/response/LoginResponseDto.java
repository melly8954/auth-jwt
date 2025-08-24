package com.melly.authjwt.dto.response;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LoginResponseDto {
    private String username;
    private String accessToken;
    private String refreshToken;
    private String role;
    private String message;
    private boolean success;
}
