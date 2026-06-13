package com.meditrip.user.presentation.dto.request;

import com.meditrip.user.application.dto.request.WithdrawnApplicationRequest;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@AllArgsConstructor
@Getter
@Builder
public class WithdrawnRequest {

    @NotBlank(message = "Password is required.")
    private String password;

    public WithdrawnApplicationRequest toApplicationRequest(){
        return WithdrawnApplicationRequest.builder()
                .password(this.password)
                .build();
    }

}
