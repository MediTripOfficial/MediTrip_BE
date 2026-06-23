package com.meditrip.user.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class VerifyEmailApplicationRequest {

    private String email;
    private String authCode;

}
