package com.meditrip.user.infra.redis;

import com.meditrip.common.util.SecurityUtils;
import com.meditrip.user.application.EmailAuthCodeStore;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
@Slf4j
public class RedisEmailAuthCodeRepository implements EmailAuthCodeStore {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String KEY_PREFIX = "auth:email:";
    private static final String TOKEN_PREFIX = "verified:email:";
    private static final long REDIS_TTL_MINUTES = 10L;
    private static final long AUTH_VALID_MINUTES = 5L;

    @Override
    public void save(String email, String authCode) {
        String maskedEmail = SecurityUtils.convertToMaskedEmail(email);
        String key = KEY_PREFIX + email;
        long now = System.currentTimeMillis();

        String value = authCode + ":" + now;

        log.info("이메일 인증 코드 레디스 저장. 이메일 : [{}], TTL : [{}분]", maskedEmail, REDIS_TTL_MINUTES);

        redisTemplate.opsForValue().set(key, value, REDIS_TTL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public void deleteByEmail(String email) {
        String key = KEY_PREFIX + email;
        log.info("이메일 인증 정보 삭제. 이메일 : [{}]", SecurityUtils.convertToMaskedEmail(email));
        redisTemplate.delete(key);
    }

    @Override
    public void saveVerifiedToken(String email, String verifyToken, int ttl) {
        String key = TOKEN_PREFIX + email;
        String maskedEmail = SecurityUtils.convertToMaskedEmail(email);

        log.info("이메일 인증 성공 증명 토큰 레디스 저장. 이메일 : [{}], TTL : [{}분]", maskedEmail, ttl);

        redisTemplate.opsForValue().set(key, verifyToken, ttl, TimeUnit.MINUTES);
    }

    @Override
    public void deleteVerifiedTokenByEmail(String email) {
        String key = TOKEN_PREFIX + email;
        log.info("이메일 인증 토큰 정보 삭제. 이메일 : [{}]", SecurityUtils.convertToMaskedEmail(email));
        redisTemplate.delete(key);
    }

}
