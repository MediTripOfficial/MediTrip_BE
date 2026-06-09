package com.meditrip.user.domain.entity.enums;

import lombok.Getter;

@Getter
public enum Gender {

    M("Male", "남성"),
    F("Female", "여성");

    private final String eng;
    private final String ko;

    Gender(String eng, String ko) {
        this.eng = eng;
        this.ko = ko;
    }

}
