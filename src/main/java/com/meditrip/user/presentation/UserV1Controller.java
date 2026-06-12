package com.meditrip.user.presentation;

import com.meditrip.common.jwt.CustomUserDetails;
import com.meditrip.user.application.UserFacade;
import com.meditrip.user.application.UserService;
import com.meditrip.user.application.dto.response.UserInfoResponse;
import com.meditrip.user.presentation.dto.request.UpdatePasswordRequest;
import com.meditrip.user.presentation.dto.request.UpdateUserInfoRequest;
import com.meditrip.user.presentation.dto.request.WithdrawnRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

    @GetMapping("/email/check")
    public ResponseEntity<String> checkEmailDuplication(@RequestParam(required = false) String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email is required.");
        }

        userService.checkEmailDuplication(email);
        return ResponseEntity.ok( "사용 가능");
    }

    @GetMapping("/nickname/check")
    public ResponseEntity<String> checkNicknameDuplication(@RequestParam(required = false) String nickname) {
        if (nickname == null || nickname.isBlank()) {
            throw new IllegalArgumentException("Nickname is required.");
        }

        userService.checkNicknameDuplication(nickname);
        return ResponseEntity.ok( "사용 가능");
    }

    @GetMapping("/me")
    public ResponseEntity<UserInfoResponse> getUserInfo(@AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(userService.getUserInfo(UUID.fromString(userDetails.getUserId())));
    }

    @PutMapping("/me")
    public ResponseEntity<UserInfoResponse> updateUserInfo(@AuthenticationPrincipal CustomUserDetails userDetails,
                                                        @Valid @RequestBody UpdateUserInfoRequest request) {
        return ResponseEntity.ok(userFacade.updateUserInfo(UUID.fromString(userDetails.getUserId()), request.toApplicationRequest()));
    }

    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteUser(@AuthenticationPrincipal CustomUserDetails userDetails,
                                           @Valid @RequestBody WithdrawnRequest withdrawnRequest) {
        userFacade.deleteUser(UUID.fromString(userDetails.getUserId()), withdrawnRequest.toApplicationRequest());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> updatePassword(@AuthenticationPrincipal CustomUserDetails userDetails,
                                            @Valid @RequestBody UpdatePasswordRequest request) {
        userFacade.updatePassword(UUID.fromString(userDetails.getUserId()), request.toApplicationRequest());
        return ResponseEntity.ok(null);
    }

}
