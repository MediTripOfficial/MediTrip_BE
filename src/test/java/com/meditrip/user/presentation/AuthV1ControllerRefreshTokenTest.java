package com.meditrip.user.presentation;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.meditrip.common.jwt.JwtProperties;
import com.meditrip.common.jwt.JwtProvider;
import com.meditrip.user.application.TokenService;
import com.meditrip.user.application.UserService;
import com.meditrip.user.domain.entity.User;
import com.meditrip.user.presentation.dto.request.TokenRefreshRequest;
import java.util.Date;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class AuthV1ControllerRefreshTokenTest extends ControllerTestSupport {

    @Autowired
    private JwtProvider jwtProvider;

    @MockitoBean
    private TokenService tokenService;

    @Autowired
    private JwtProperties jwtProperties;

    @MockitoBean
    private UserService userService;

    @DisplayName("올바른 Refresh Token으로 재발급을 요청하면 새로운 토큰 세트를 반환한다.")
    @Test
    void shouldReturnNewTokens_whenRefreshTokenIsValid() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String originalRefreshToken = jwtProvider.generateRefreshToken(userId.toString());

        TokenRefreshRequest request = new TokenRefreshRequest(originalRefreshToken);

        User mockUser = mock(User.class);
        given(userService.findById(userId, "토큰 재발급")).willReturn(mockUser);
        given(tokenService.getRefreshToken(userId)).willReturn(originalRefreshToken);

        //when, then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists());
    }

    @DisplayName("위조되거나 잘못된 형식의 Refresh Token으로 재발급을 요청하면 401을 반환한다.")
    @Test
    void shouldReturnInvalidTokenCode_whenRefreshTokenIsMalformedOrForged() throws Exception {
        //given
        String malformedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.invalid.payload.signature";
        TokenRefreshRequest request = new TokenRefreshRequest(malformedToken);

        //when, then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid refresh token."));
    }

    @DisplayName("만료 시간이 지난(Expired) Refresh Token으로 재발급을 요청하면 401을 반환한다.")
    @Test
    void shouldReturnExpiredTokenCode_whenRefreshTokenIsExpired() throws Exception {
        //given
        UUID userId = UUID.randomUUID();

        String expiredRefreshToken = io.jsonwebtoken.Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date(System.currentTimeMillis() - 60000))
                .claim(jwtProperties.tokenTypeClaim(), jwtProperties.refreshTokenType())
                .expiration(new Date(System.currentTimeMillis() - 1000))
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor(
                        jwtProperties.secret().getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .compact();

        TokenRefreshRequest request = new TokenRefreshRequest(expiredRefreshToken);

        //when, then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token has expired."));
    }

    @DisplayName("Refresh Token 구조는 정상이나 Redis에 저장된 토큰과 다르면 401을 반환한다.")
    @Test
    void shouldReturnInvalidTokenCode_whenRefreshTokenDoesNotMatchInRedis() throws Exception {
        //given
        UUID userId = UUID.randomUUID();
        String clientRefreshToken = jwtProvider.generateRefreshToken(userId.toString());
        String dbRefreshToken = "different-stored-token";

        TokenRefreshRequest request = new TokenRefreshRequest(clientRefreshToken);

        User mockUser = mock(User.class);
        given(userService.findById(userId, "토큰 재발급")).willReturn(mockUser);
        given(tokenService.getRefreshToken(eq(userId))).willReturn(dbRefreshToken);

        //when, then
        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid refresh token."));
    }

}
