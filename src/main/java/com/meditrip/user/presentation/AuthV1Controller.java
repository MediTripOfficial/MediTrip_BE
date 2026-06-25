package com.meditrip.user.presentation;

import com.meditrip.common.jwt.CustomUserDetails;
import com.meditrip.user.application.AuthFacade;
import com.meditrip.user.application.dto.response.TokenResponse;
import com.meditrip.user.presentation.dto.request.LoginRequest;
import com.meditrip.user.presentation.dto.request.ResetPasswordRequest;
import com.meditrip.user.presentation.dto.request.SignupRequest;
import com.meditrip.user.presentation.dto.request.TokenRefreshRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthV1Controller {

    private final AuthFacade authFacade;

    @PostMapping("/signup/local")
    @Operation(summary = "로컬 회원가입")
    public ResponseEntity<TokenResponse> signup(@Valid @RequestBody SignupRequest signupRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authFacade.signup(signupRequest.toApplicationRequest()));
    }

    @PostMapping("/login")
    @Operation(summary = "로컬 로그인")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        return ResponseEntity.ok(authFacade.login(loginRequest.toApplicationRequest()));
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃")
    public ResponseEntity<Void> logout(@AuthenticationPrincipal CustomUserDetails userDetails){
        UUID userId = UUID.fromString(userDetails.getUserId());
        authFacade.logout(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "토큰 재발급", description = "만료된 Access Token을 Refresh Token을 통해 재발급합니다.")
    public ResponseEntity<TokenResponse> reissueToken(@RequestBody @Valid TokenRefreshRequest request) {
        TokenResponse response = authFacade.reissue(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/password")
    @Operation(summary = "비밀번호 초기화")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        authFacade.resetPassword(request.toApplicationRequest());
        return ResponseEntity.ok(null);
    }

}
