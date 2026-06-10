package com.meditrip.user.application;

import java.time.Duration;
import java.util.UUID;

public interface TokenStore {

    void save(UUID userId, String refreshToken, Duration refreshTokenTtl);
    String findByUserId(UUID userId);
    void delete(UUID userId);

}
