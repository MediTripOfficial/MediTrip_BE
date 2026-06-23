package com.meditrip.user.presentation;

import com.meditrip.common.jwt.CustomUserDetails;
import com.meditrip.user.application.EmailService;
import com.meditrip.user.application.UserFacade;
import com.meditrip.user.application.UserService;
import com.meditrip.user.application.dto.response.UserInfoResponse;
import com.meditrip.user.presentation.dto.request.OnboardingRequest;
import com.meditrip.user.presentation.dto.request.SendVerifyEmailRequest;
import com.meditrip.user.presentation.dto.request.UpdatePasswordRequest;
import com.meditrip.user.presentation.dto.request.UpdateUserInfoRequest;
import com.meditrip.user.presentation.dto.request.WithdrawnRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserV1Controller {

    private final UserService userService;
    private final UserFacade userFacade;
    private final EmailService emailService;

    @GetMapping("/email/check")
    @Operation(summary = "이메일 중복 검사")
    public ResponseEntity<String> checkEmailDuplication(@RequestParam(required = false) String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }

        userService.checkEmailDuplication(email);
        return ResponseEntity.ok("사용 가능");
    }

    @GetMapping("/nickname/check")
    @Operation(summary = "닉네임 중복 검사")
    public ResponseEntity<String> checkNicknameDuplication(@RequestParam(required = false) String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("Nickname is required.");
        }

        userService.checkNicknameDuplication(nickname);
        return ResponseEntity.ok("사용 가능");
    }

    @GetMapping("/me")
    @Operation(summary = "유저 정보 조회")
    public ResponseEntity<UserInfoResponse> getUserInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(userService.getUserInfo(UUID.fromString(userDetails.getUserId())));
    }

    @PutMapping("/me")
    @Operation(summary = "유저 정보 업데이트")
    public ResponseEntity<UserInfoResponse> updateUserInfo(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                           @Valid @RequestBody UpdateUserInfoRequest request) {
        return ResponseEntity.ok(
                userFacade.updateUserInfo(UUID.fromString(userDetails.getUserId()), request.toApplicationRequest()));
    }

    @DeleteMapping("/me")
    @Operation(summary = "유저 탈퇴")
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal CustomUserDetails userDetails,
                                           @Valid @RequestBody WithdrawnRequest withdrawnRequest) {
        userFacade.deleteUser(UUID.fromString(userDetails.getUserId()), withdrawnRequest.toApplicationRequest());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/password")
    @Operation(summary = "유저 비밀번호 변경")
    public ResponseEntity<Void> updatePassword(@AuthenticationPrincipal CustomUserDetails userDetails,
                                               @Valid @RequestBody UpdatePasswordRequest request) {
        userFacade.updatePassword(UUID.fromString(userDetails.getUserId()), request.toApplicationRequest());
        return ResponseEntity.ok(null);
    }

    @PostMapping("/onboarding")
    @Operation(summary = "소셜 로그인 온보딩")
    public ResponseEntity<UserInfoResponse> onboarding(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                       @Valid @RequestBody OnboardingRequest onboardingRequest) {
        return ResponseEntity.ok(userFacade.onboarding(UUID.fromString(userDetails.getUserId()),
                onboardingRequest.toApplicationRequest()));
    }

    @PostMapping("/email/verification")
    @Operation(summary = "이메일 인증 코드 전송")
    public ResponseEntity<Void> sendVerifyEmail(@Valid @RequestBody SendVerifyEmailRequest request) {
        emailService.sendVerifyEmail(request.getEmail());
        return ResponseEntity.ok(null);
    }

}
