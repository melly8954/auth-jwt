package com.melly.authjwt.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class ReIssueTokenDto {
    private String newAccessToken;
    private String newRefreshToken;
}
