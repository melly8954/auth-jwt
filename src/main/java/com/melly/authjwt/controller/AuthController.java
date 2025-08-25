package com.melly.authjwt.controller;

import com.melly.authjwt.common.controller.ResponseController;
import com.melly.authjwt.common.dto.ResponseDto;
import com.melly.authjwt.dto.request.LoginRequestDto;
import com.melly.authjwt.dto.response.LoginResponseDto;
import com.melly.authjwt.dto.response.ReIssueTokenDto;
import com.melly.authjwt.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthController implements ResponseController {
    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<ResponseDto<LoginResponseDto>> jwtLogin(@RequestBody LoginRequestDto dto, HttpServletResponse response) {
        LoginResponseDto responseDto = authService.login(dto,response);
        return makeResponseEntity(HttpStatus.OK, null, responseDto.getMessage(), responseDto);
    }

    @PostMapping("/reissue")
    public ResponseEntity<ResponseDto<ReIssueTokenDto>> reissueToken(HttpServletRequest request, HttpServletResponse response){
        ReIssueTokenDto responseDto = authService.reissueToken(request, response);
        return makeResponseEntity(HttpStatus.OK, "null","토큰 재발급 성공", responseDto);
    }
}
