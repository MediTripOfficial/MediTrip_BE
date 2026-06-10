package com.meditrip.user.presentation.dto.request;

import com.meditrip.user.application.dto.request.LoginApplicationRequest;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
@AllArgsConstructor
public class LoginRequest {

    @NotNull(message = "Email is required.")
    @Pattern(
            regexp = "^[a-zA-Z0-9+\\-_.]+@[a-zA-Z0-9-]+\\.[a-zA-Z]{2,}$",
            message = "Please enter a valid email address."
    )
    private String email;

    @NotNull(message = "Password is required.")
    @Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]+$",
            message = "Password must include letters, numbers, and special characters."
    )
    private String password;

    public LoginApplicationRequest toApplicationRequest(){
        return LoginApplicationRequest.builder()
                .email(this.email)
                .password(this.password)
                .build();
    }

}
