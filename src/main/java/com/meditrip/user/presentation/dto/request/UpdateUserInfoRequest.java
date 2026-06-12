package com.meditrip.user.presentation.dto.request;

import com.meditrip.user.application.dto.request.UpdateUserInfoApplicationRequest;
import com.meditrip.user.domain.entity.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder(toBuilder = true)
public class UpdateUserInfoRequest {

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

    public UpdateUserInfoApplicationRequest toApplicationRequest(){
        return UpdateUserInfoApplicationRequest.builder()
                .name(this.name)
                .nickname(nickname)
                .weight(this.weight)
                .height(this.height)
                .gender(Gender.valueOf(this.gender.toUpperCase()))
                .birth(this.birth)
                .country(this.country)
                .underlyingDisease(this.underlyingDisease)
                .allergies(this.allergies)
                .isMarketingTermsAgreed(this.isMarketingTermsAgreed)
                .profileImg(this.profileImg)
                .build();
    }

}
