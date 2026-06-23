package com.meditrip.user.application;

import java.util.Optional;

public interface EmailAuthCodeStore {
    void save(String email, String authCode);

    void deleteByEmail(String email);

    void saveVerifiedToken(String email, String verifyToken, int ttl);

    void deleteVerifiedTokenByEmail(String email);

    Optional<String> findByEmail(String email);

}
