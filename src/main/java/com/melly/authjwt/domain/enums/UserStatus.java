package com.melly.authjwt.domain.enums;

import lombok.Getter;

@Getter
public enum UserStatus {
    ACTIVE("계정 활성화"),
    INACTIVE("계정 비활성화"),
    DELETED("계정 탈퇴");

    private final String description;

    UserStatus(String description) {
        this.description = description;
    }
}
