package com.meditrip.user.infra.redis;

import com.meditrip.common.util.RateLimiter;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class RedisRateLimiter implements RateLimiter {

    private final RedisTemplate<String, String> redisTemplate;

    @Override
    public boolean tryAcquire(String key, int maxCount, Duration window) {
        Long count = redisTemplate.opsForValue().increment(key);

        if (count == null) {
            return true;
        }

        if (count == 1L) {
            redisTemplate.expire(key, window);
        }

        return count <= maxCount;
    }

}
