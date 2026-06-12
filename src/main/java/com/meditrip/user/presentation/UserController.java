package com.meditrip.user.presentation;

import com.meditrip.common.jwt.CustomUserDetails;
import com.meditrip.user.application.UserService;
import com.meditrip.user.application.dto.response.UserInfoResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

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

}
