package com.melly.authjwt.domain.enums;

import lombok.Getter;

@Getter
public enum UserRole {
    USER("사용자"),
    ADMIN("관리자");

    private final String description;

    UserRole(String description) {
        this.description = description;
    }
}

