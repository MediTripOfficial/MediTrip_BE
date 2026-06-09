package com.meditrip.user.domain.entity.enums;

import lombok.Getter;

@Getter
public enum UserStatus {
    GUEST("게스트"),
    WITHDRAWN("탈퇴"),
    DELETED("삭제"),
    ACTIVE("정상");

    private final String description;

    UserStatus(String description) {
        this.description = description;
    }

}
