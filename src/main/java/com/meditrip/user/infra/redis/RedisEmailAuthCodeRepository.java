package com.meditrip.user.infra.redis;

import com.meditrip.common.util.SecurityUtils;
import com.meditrip.user.application.EmailAuthCodeStore;
import java.util.Optional;
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

    private static final String KEY_PREFIX = "mediTrip:auth:email:";
    private static final String TOKEN_PREFIX = "mediTrip:verified:email:";
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

    @Override
    public Optional<String> findByEmail(String email) {
        String key = KEY_PREFIX + email;
        String value = redisTemplate.opsForValue().get(key);

        if (value == null) {
            log.warn("이메일 인증 정보가 존재하지 않거나 만료되었습니다. 이메일 : [{}]", SecurityUtils.convertToMaskedEmail(email));
            throw new IllegalArgumentException("Invalid verification code.");
        }

        String[] parts = value.split(":");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid verification code.");
        }

        String authCode = parts[0];
        long createdAt;
        try {
            createdAt = Long.parseLong(parts[1]);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid verification code.");
        }

        long current = System.currentTimeMillis();
        long diffMinutes = (current - createdAt) / (1000 * 60);

        if (diffMinutes >= AUTH_VALID_MINUTES) {
            log.info("이메일 인증 유효 시간(5분)이 지났습니다. 이메일 : [{}], 경과 시간 : [{}분]",
                    SecurityUtils.convertToMaskedEmail(email), diffMinutes);
            throw new IllegalArgumentException("The verification code has expired.");
        }

        return Optional.of(authCode);
    }

}
