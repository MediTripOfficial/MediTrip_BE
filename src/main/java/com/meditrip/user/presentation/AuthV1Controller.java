package com.meditrip.user.presentation;

import com.meditrip.user.application.AuthFacade;
import com.meditrip.user.application.dto.response.TokenResponse;
import com.meditrip.user.presentation.dto.request.SignupRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

}
