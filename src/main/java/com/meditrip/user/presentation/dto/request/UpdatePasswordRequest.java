package com.meditrip.user.presentation.dto.request;

import com.meditrip.user.application.dto.request.UpdatePasswordApplicationRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class UpdatePasswordRequest {

    @NotBlank(message = "Existing password is required.")
    private String existingPassword;

    @NotNull(message = "New password is required.")
    @Size(min = 8, max = 20, message = "Password must be between 8 and 20 characters.")
    @Pattern(
            regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]+$",
            message = "Password must include letters, numbers, and special characters."
    )
    private String newPassword;

    public UpdatePasswordApplicationRequest toApplicationRequest(){
        return UpdatePasswordApplicationRequest.builder()
                .existingPassword(this.existingPassword)
                .newPassword(this.newPassword)
                .build();
    }

}
