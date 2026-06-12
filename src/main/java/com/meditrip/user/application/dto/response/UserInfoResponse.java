package com.meditrip.user.application.dto.response;

import com.meditrip.user.domain.entity.User;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UserInfoResponse {

    private final String email;
    private final String profileImg;
    private final String name;
    private final String nickname;
    private final LocalDate birth;
    private final String gender;
    private final String country;
    private final Double weight;
    private final Double height;
    private final boolean isMarketingTermsAgreed;
    private final List<String> underlyingDisease;
    private final List<String> allergies;

    public static UserInfoResponse from(User user, List<String> underlyingDisease, List<String> allergies) {
        return UserInfoResponse.builder()
                .email(user.getEmail())
                .profileImg(user.getProfileImg())
                .name(user.getName())
                .nickname(user.getNickname())
                .birth(user.getBirth())
                .gender(user.getGender().getEng())
                .country(user.getCountry())
                .weight(user.getWeight())
                .height(user.getHeight())
                .isMarketingTermsAgreed(user.getIsMarketingTermsAgreed())
                .underlyingDisease(underlyingDisease)
                .allergies(allergies)
                .build();
    }

}
