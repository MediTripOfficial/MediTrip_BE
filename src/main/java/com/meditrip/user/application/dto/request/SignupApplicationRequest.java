package com.meditrip.user.application.dto.request;

import com.meditrip.user.domain.entity.enums.Gender;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class SignupApplicationRequest {

    private String email;
    private String password;
    private String name;
    private String nickname;
    private Double weight;
    private Double height;
    private LocalDate birth;
    private Gender gender;
    private String country;
    private List<String> underlyingDisease;
    private List<String> allergies;
    private boolean isMarketingTermsAgreed;
    private UUID verifiedToken;
    private String profileImg;

}
