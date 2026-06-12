package com.meditrip.user.application.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class UpdatePasswordApplicationRequest {

    private String existingPassword;
    private String newPassword;

}
