package com.meditrip.common.jwt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.meditrip.common.exception.JwtAuthenticationException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class JwtProviderTest {

    private static final String SECRET = "asdfawerqwer123456adfasdfqwer123456afasdfasdfasfdqerq34rq34fadssr34efwasdfawerwfesdfd";
    private JwtProvider jwtProvider;

    private static final String USER_ID = "UUID-123";

    @BeforeEach
    void setUp() throws Exception {
        JwtProperties jwtProperties = new JwtProperties(
                SECRET,
                3600000L,
                604800000L,
                "token_type",
                "access",
                "refresh",
                "test:refreshToken:"
        );

        jwtProvider = new JwtProvider(jwtProperties);
    }

    @DisplayName("access Token을 생성할 수 있다.")
    @Test
    void shouldGenerateAccessToken_successfully() {
        //when
        String token = jwtProvider.generateAccessToken(USER_ID);

        //then
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @DisplayName("refresh Token을 생성할 수 있다.")
    @Test
    void shouldGenerateRefreshToken_successfully() {
        //when
        String token = jwtProvider.generateRefreshToken(USER_ID);

        //then
        assertThat(token).isNotNull();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @DisplayName("토큰에서 userId를 추출할 수 있다.")
    @Test
    void shouldExtractUserIdFromToken_successfully() {
        //given
        String token = jwtProvider.generateAccessToken(USER_ID);

        //when
        String extractedUserId = jwtProvider.getUserId(token);

        //then
        assertThat(extractedUserId).isEqualTo(USER_ID);
    }

    @DisplayName("유효한 토큰은 검증을 통과한다.")
    @Test
    void shouldPassValidation_whenTokenIsValid() {
        //given
        String token = jwtProvider.generateAccessToken(USER_ID);

        //when
        boolean validateToken = jwtProvider.validateAccessToken(token);

        //then
        assertThat(validateToken).isTrue();
    }

    @Test
    @DisplayName("만료된 토큰은 예외를 던진다.")
    void shouldThrowException_whenTokenIsExpired() throws Exception {
        //given
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String expiredToken = Jwts.builder()
                .subject(USER_ID)
                .issuedAt(new Date(System.currentTimeMillis() - 10000))
                .expiration(new Date(System.currentTimeMillis() - 5000))
                .signWith(key)
                .compact();

        //when, then
        assertThatThrownBy(() -> jwtProvider.validateAccessToken(expiredToken))
                .isInstanceOf(JwtAuthenticationException.class)
                .satisfies(e -> {
                    JwtAuthenticationException ex = (JwtAuthenticationException) e;
                    assertThat(ex.getMessage()).isEqualTo("올바르지 않은 토큰입니다.");
                });
    }

    @Test
    @DisplayName("위조된 토큰은 예외를 던진다.")
    void shouldThrowException_whenTokenIsInvalid() {
        //given
        String invalidToken = "this.is.invalid";

        //when, then
        assertThatThrownBy(() -> jwtProvider.validateAccessToken(invalidToken))
                .isInstanceOf(JwtAuthenticationException.class)
                .satisfies(e -> {
                    JwtAuthenticationException ex = (JwtAuthenticationException) e;
                    assertThat(ex.getMessage()).isEqualTo("올바르지 않은 토큰입니다.");
                });
    }

    @Test
    @DisplayName("다른 시크릿으로 서명된 토큰은 예외를 던진다.")
    void shouldThrowException_whenTokenIsSignedWithWrongSecret() {
        //given
        SecretKey wrongKey = Keys.hmacShaKeyFor(
                "wrong-secret-key-must-be-at-least-32-bytes!!".getBytes(StandardCharsets.UTF_8)
        );
        String wrongToken = Jwts.builder()
                .subject(USER_ID)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + 10000))
                .signWith(wrongKey)
                .compact();

        //when, then
        assertThatThrownBy(() -> jwtProvider.validateAccessToken(wrongToken))
                .isInstanceOf(JwtAuthenticationException.class)
                .satisfies(e -> {
                    JwtAuthenticationException ex = (JwtAuthenticationException) e;
                    assertThat(ex.getMessage()).isEqualTo("올바르지 않은 토큰입니다.");
                });
    }

}
