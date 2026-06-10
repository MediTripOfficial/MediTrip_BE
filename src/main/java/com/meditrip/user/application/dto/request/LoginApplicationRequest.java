package com.meditrip.user.application.dto.request;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class LoginApplicationRequest {

    private String email;
    private String password;

}
