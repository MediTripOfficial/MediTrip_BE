package com.meditrip.user.presentation.dto.request;

import com.meditrip.user.application.dto.request.VerifyEmailApplicationRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Builder
@Getter
public class VerifyEmailRequest {

    @NotNull(message = "Email is required.")
    @Pattern(
            regexp = "^[a-zA-Z0-9+\\-_.]+@[a-zA-Z0-9-]+\\.[a-zA-Z]{2,}$",
            message = "Please enter a valid email address."
    )
    private String email;

    @NotBlank(message = "Auth code is required.")
    private String authCode;

    public VerifyEmailApplicationRequest toApplicationRequest(){
        return VerifyEmailApplicationRequest.builder()
                .email(this.email)
                .authCode(this.authCode)
                .build();
    }

}
