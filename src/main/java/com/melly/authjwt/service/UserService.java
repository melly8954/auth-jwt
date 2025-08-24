package com.melly.authjwt.service;

import com.melly.authjwt.dto.request.SignUpRequestDto;

public interface UserService {
    void signUp(SignUpRequestDto dto);
}
