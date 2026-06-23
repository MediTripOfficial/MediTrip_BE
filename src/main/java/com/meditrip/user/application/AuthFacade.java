package com.meditrip.user.application;

import com.meditrip.common.exception.JwtAuthenticationException;
import com.meditrip.common.jwt.JwtProvider;
import com.meditrip.common.util.SecurityUtils;
import com.meditrip.user.application.dto.request.LoginApplicationRequest;
import com.meditrip.user.application.dto.request.SignupApplicationRequest;
import com.meditrip.user.application.dto.response.TokenResponse;
import com.meditrip.user.domain.entity.User;
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
    private final UserService userService;

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

    public TokenResponse login(LoginApplicationRequest request) {
        String maskedEmail = SecurityUtils.convertToMaskedEmail(request.getEmail());
        log.info("로그인 요청. Email : [{}]", maskedEmail);

        User user = userService.findLoginUserByEmail("로그인", request.getEmail());
        user.validateStatusForLogin();
        authService.verifyPasswordForLogin(request.getPassword(), user.getPassword());

        String accessToken = jwtProvider.generateAccessToken(user.getId().toString());
        String refreshToken = jwtProvider.generateRefreshToken(user.getId().toString());

        tokenService.saveRefreshToken(user.getId(), refreshToken);

        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public void logout(UUID userId) {
        tokenService.deleteRefreshToken(userId);
    }

    public TokenResponse reissue(String refreshToken) {
        jwtProvider.validateRefreshToken(refreshToken);

        String userIdStr = jwtProvider.getUserId(refreshToken);
        UUID userId = UUID.fromString(userIdStr);

        User user = userService.findById(userId, "토큰 재발급");
        user.validateStatusForLogin();

        String storedRefreshToken = tokenService.getRefreshToken(userId);

        if (storedRefreshToken == null || !storedRefreshToken.equals(refreshToken)) {
            log.warn("토큰 재발급 실패. Redis에 저장된 토큰과 일치하지 않거나 이미 로그아웃된 유저. User Id : [{}]", userId);
            throw new JwtAuthenticationException("Invalid refresh token.");
        }

        String newAccessToken = jwtProvider.generateAccessToken(userIdStr);
        String newRefreshToken = jwtProvider.generateRefreshToken(userIdStr);

        tokenService.saveRefreshToken(userId, newRefreshToken);

        return TokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }

}
