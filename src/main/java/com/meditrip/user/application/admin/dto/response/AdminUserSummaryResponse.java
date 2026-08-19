package com.meditrip.user.application.admin.dto.response;

import com.meditrip.common.domain.UserStatus;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AdminUserSummaryResponse {

    private final UUID userId;
    private final String email;
    private final String name;
    private final String nickname;
    private final Double weight;
    private final Double height;
    private final LocalDate birth;
    private final String gender;
    private final String country;
    private final UserStatus status;
    private final Boolean isMarketingTermsAgreed;

}
