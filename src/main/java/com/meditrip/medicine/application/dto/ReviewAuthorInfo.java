package com.meditrip.medicine.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class ReviewAuthorInfo {

    private final String nickname;
    private final String profileImg;
    private final Integer age;

}
