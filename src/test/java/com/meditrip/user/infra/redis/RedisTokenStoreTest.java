package com.meditrip.user.infra.redis;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class RedisTokenStoreTest {

    @Autowired
    private RedisTokenStore redisTokenStore;

    @DisplayName("리프레시 토큰을 성공적으로 저장하고 UUID 기반 유저 ID로 조회할 수 있다.")
    @Test
    void shouldSaveAndFindRefreshToken_byUserId() {
        //given
        UUID userId = UUID.randomUUID();
        String refreshToken = "sample-refresh-token-value";
        Duration ttl = Duration.ofDays(7);

        //when
        redisTokenStore.save(userId, refreshToken, ttl);
        String foundToken = redisTokenStore.findByUserId(userId);

        //then
        assertThat(foundToken).isNotNull();
        assertThat(foundToken).isEqualTo(refreshToken);
    }

    @DisplayName("존재하지 않는 유저 ID로 토큰을 조회하면 null을 반환한다.")
    @Test
    void shouldReturnNull_whenTokenDoesNotExist() {
        //given
        UUID userId = UUID.randomUUID();

        //when
        String foundToken = redisTokenStore.findByUserId(userId);

        //then
        assertThat(foundToken).isNull();
    }

    @DisplayName("토큰 삭제 요청 시 레디스에서 해당 유저의 토큰 데이터가 완전히 제거된다.")
    @Test
    void shouldDeleteToken_successfully() {
        //given
        UUID userId = UUID.randomUUID();
        String refreshToken = "token-to-be-deleted";
        redisTokenStore.save(userId, refreshToken, Duration.ofMinutes(10));

        //when
        redisTokenStore.delete(userId);
        String foundToken = redisTokenStore.findByUserId(userId);

        //then
        assertThat(foundToken).isNull();
    }

}
