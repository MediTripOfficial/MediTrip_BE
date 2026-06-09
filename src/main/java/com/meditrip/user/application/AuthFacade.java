package com.meditrip.user.application;

import com.meditrip.common.jwt.JwtProvider;
import com.meditrip.user.application.dto.request.SignupApplicationRequest;
import com.meditrip.user.application.dto.response.TokenResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthFacade {

    private final AuthService authService;
    private final JwtProvider jwtProvider;
    private final TokenService tokenService;

    public TokenResponse signup(SignupApplicationRequest request) {
        //TODO : 이메일 인증 기능 구현 후 verifiedToken 검증 추가

        UUID userId = authService.signup(request);

        String accessToken = jwtProvider.generateAccessToken(userId.toString());
        String refreshToken = jwtProvider.generateRefreshToken(userId.toString());

        tokenService.saveRefreshToken(userId, refreshToken);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

}
