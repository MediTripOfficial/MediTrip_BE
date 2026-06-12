package com.meditrip.user.application;

import com.meditrip.user.application.dto.request.UpdateUserInfoApplicationRequest;
import com.meditrip.user.application.dto.response.UserInfoResponse;
import com.meditrip.user.domain.entity.User;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class UserFacade {

    private final UserService userService;
    private final AuthService authService;

    @Transactional
    public UserInfoResponse updateUserInfo(UUID userId, UpdateUserInfoApplicationRequest request) {
        log.info("유저 정보 업데이트 요청. User Id : [{}], 업데이트 요청 정보 : [{}]", userId, request.toString());
        User user = userService.findById(userId, "유저 정보 업데이트");
        user.validateStatus();

        userService.validUpdateUser(request.getNickname());

        user.updateInfo(request.getName(), request.getNickname(), request.getWeight(), request.getHeight(),
                request.getBirth(), request.getGender(), request.getCountry(), request.isMarketingTermsAgreed(),
                request.getProfileImg());

        userService.deleteAllergyAndConditions(userId);

        authService.saveAllergies(request.getAllergies(), userId);
        authService.saveConditions(request.getUnderlyingDisease(), userId);

        return userService.getUserInfo(userId);
    }

}
