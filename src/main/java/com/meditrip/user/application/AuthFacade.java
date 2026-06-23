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
    private final EmailAuthCodeStore emailAuthCodeStore;

    public TokenResponse signup(SignupApplicationRequest request) {
        String email = request.getEmail();
        String verifiedToken = emailAuthCodeStore.findVerifiedTokenByEmail(email);

        validEmailVerificationToken(verifiedToken, email, request.getVerifiedToken());

        UUID userId = authService.signup(request);

        String accessToken = jwtProvider.generateAccessToken(userId.toString());
        String refreshToken = jwtProvider.generateRefreshToken(userId.toString());

        tokenService.saveRefreshToken(userId, refreshToken);

        emailAuthCodeStore.deleteVerifiedTokenByEmail(email);

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
        userService.findById(userId, "토큰 재발급");

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

    private void validEmailVerificationToken(String verifiedToken, String email, String request) {
        if (verifiedToken.isEmpty()) {
            log.warn("유효한 이메일 인증 토큰이 존재하지 않거나 만료되었습니다. 이메일 : [{}]", SecurityUtils.convertToMaskedEmail(email));
            throw new IllegalArgumentException("Email verification token is missing or has expired.");
        }

        if (!verifiedToken.equals(request)) {
            log.info("이메일 인증 토큰이 동일하지 않습니다. 이메일 : [{}]", SecurityUtils.convertToMaskedEmail(email));
            throw new IllegalArgumentException("Email verification token is missing or has expired.");
        }
    }

}
