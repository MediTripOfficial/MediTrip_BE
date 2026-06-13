package com.meditrip.user.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class TokenRefreshRequest {

    @NotBlank(message = "리프레시 토큰은 필수 입력 값입니다.")
    private String refreshToken;

}
