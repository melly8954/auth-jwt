package com.melly.authjwt.controller;

import com.melly.authjwt.common.controller.ResponseController;
import com.melly.authjwt.common.dto.ResponseDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/admins")
public class AdminController implements ResponseController {
    @GetMapping("test")
    public ResponseEntity<ResponseDto<String>> test() {
        return makeResponseEntity(HttpStatus.OK, null, "admins 테스트 성공", "admins ok");
    }

}

