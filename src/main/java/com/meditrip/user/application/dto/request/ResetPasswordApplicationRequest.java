package com.meditrip.user.application.dto.request;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ResetPasswordApplicationRequest {

    private String email;
    private String password;
    private String verifiedToken;
    
}
