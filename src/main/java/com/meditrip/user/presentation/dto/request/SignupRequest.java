package com.meditrip.user.presentation.dto.request;


import com.meditrip.user.application.dto.request.SignupApplicationRequest;
import com.meditrip.user.domain.entity.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
public class SignupRequest {

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

    @NotBlank(message = "Name is required.")
    @Size(min = 2, max = 20, message = "Name must be between 2 and 20 characters.")
    private String name;

    @NotBlank(message = "Nickname is required.")
    @Size(min = 2, max = 12, message = "Nickname must be between 2 and 12 characters.")
    private String nickname;

    @NotNull(message = "Weight is required.")
    private Double weight;

    @NotNull(message = "Height is required.")
    private Double height;

    @NotNull(message = "Date of birth is required.")
    private LocalDate birth;

    @NotNull(message = "Gender is required.")
    private String gender;

    @NotBlank(message = "Country is required.")
    private String country;

    private List<String> underlyingDisease;
    private List<String> allergies;
    private boolean isMarketingTermsAgreed;
    private String profileImg;
    private String verifiedToken;

    public SignupApplicationRequest toApplicationRequest() {
        return SignupApplicationRequest.builder()
                .email(this.email)
                .password(this.password)
                .name(this.name)
                .nickname(this.nickname)
                .weight(this.weight)
                .height(this.height)
                .birth(this.birth)
                .gender(Gender.valueOf(this.gender.toUpperCase()))
                .country(this.country)
                .underlyingDisease(this.underlyingDisease)
                .allergies(this.allergies)
                .isMarketingTermsAgreed(this.isMarketingTermsAgreed)
                .verifiedToken(this.verifiedToken)
                .profileImg(this.profileImg)
                .build();
    }

}
