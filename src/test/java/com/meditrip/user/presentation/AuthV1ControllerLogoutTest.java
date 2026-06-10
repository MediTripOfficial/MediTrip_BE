package com.meditrip.user.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.google.common.net.HttpHeaders;
import com.meditrip.common.jwt.JwtProperties;
import com.meditrip.common.jwt.JwtProvider;
import com.meditrip.user.infra.redis.RedisTokenStore;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AuthV1ControllerLogoutTest extends ControllerTestSupport {

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private RedisTokenStore redisTokenStore;

    @Autowired
    private JwtProperties jwtProperties;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    @DisplayName("로그아웃에 성공하면 refresh token이 삭제된다.")
    @Test
    void shouldDeleteRefreshToken_whenLogoutSucceeds() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String refreshToken = jwtProvider.generateRefreshToken(userId.toString());
        String accessToken = jwtProvider.generateAccessToken(userId.toString());

        redisTokenStore.save(userId, refreshToken, Duration.ofMinutes(1));

        assertThat(redisTokenStore.findByUserId(userId)).isNotNull();

        //when
        mockMvc.perform(post("/api/v1/auth/logout")
                .header(HttpHeaders.AUTHORIZATION, "Bearer "+accessToken))
                .andExpect(status().isNoContent());

        //then
        assertThat(redisTokenStore.findByUserId(userId)).isNull();
    }

    @DisplayName("만료된 accessToken으로 로그아웃을 요청하면 401을 반환한다.")
    @Test
    void shouldReturn401Unauthorized_whenLogoutWithExpiredAccessToken() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String refreshToken = jwtProvider.generateRefreshToken(userId.toString());
        String accessToken = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date())
                .claim(jwtProperties.tokenTypeClaim(), jwtProperties.accessTokenType())
                .expiration(new Date(System.currentTimeMillis() - jwtProperties.accessTokenExpiration()))
                .signWith(getSigningKey())
                .compact();

        redisTokenStore.save(userId, refreshToken, Duration.ofMinutes(1));

        assertThat(redisTokenStore.findByUserId(userId)).isNotNull();

        //when, then
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer "+accessToken))
                .andExpect(status().isUnauthorized());

        //then
        assertThat(redisTokenStore.findByUserId(userId)).isNotNull();
    }

}
