package com.meditrip.user.application;

public interface EmailAuthCodeStore {
    void save(String email, String authCode);

    void deleteByEmail(String email);

    void saveVerifiedToken(String email, String verifyToken, int ttl);

    void deleteVerifiedTokenByEmail(String email);

}
