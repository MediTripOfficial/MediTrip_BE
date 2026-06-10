package com.meditrip.user.application;

import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenService {

    private final TokenStore tokenStore;

    @Value("${jwt.refresh-token-expiration}")
    private Duration refreshTokenTtl;

    public void saveRefreshToken(UUID userId, String refreshToken) {
        tokenStore.save(userId, refreshToken, refreshTokenTtl);
        log.info("Refresh Token 저장 완료. userId : {}", userId);
    }

    public String getRefreshToken(UUID userId) {
        return tokenStore.findByUserId(userId);
    }

    public void deleteRefreshToken(UUID userId) {
        tokenStore.delete(userId);
        log.info("Refresh Token 삭제 완료. userId : {}", userId);
    }

}
