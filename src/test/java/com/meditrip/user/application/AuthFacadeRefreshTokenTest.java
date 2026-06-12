package com.meditrip.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;

import com.meditrip.common.exception.JwtAuthenticationException;
import com.meditrip.common.jwt.JwtProvider;
import com.meditrip.user.application.dto.response.TokenResponse;
import com.meditrip.user.domain.entity.User;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthFacadeRefreshTokenTest {

    @InjectMocks
    private AuthFacade authFacade;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private TokenService tokenService;

    @Mock
    private UserService userService;

    @DisplayName("올바른 Refresh Token이 주어지면 새로운 토큰 세트를 정상적으로 발급한다.")
    @Test
    void shouldReturnNewTokenResponse_whenRefreshTokenIsValid() {
        //given
        String clientRefreshToken = "valid.refresh.token";
        UUID userId = UUID.randomUUID();
        String userIdStr = userId.toString();

        String expectedAccessToken = "new.access.token";
        String expectedRefreshToken = "new.refresh.token";

        User user = mock(User.class);
        given(userService.findById(eq(userId), anyString())).willReturn(user);

        given(jwtProvider.getUserId(eq(clientRefreshToken))).willReturn(userIdStr);
        given(tokenService.getRefreshToken(any(UUID.class))).willReturn(clientRefreshToken);

        given(jwtProvider.generateAccessToken(eq(userIdStr))).willReturn(expectedAccessToken);
        given(jwtProvider.generateRefreshToken(eq(userIdStr))).willReturn(expectedRefreshToken);

        //when
        TokenResponse response = authFacade.reissue(clientRefreshToken);

        //then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo(expectedAccessToken);
        assertThat(response.getRefreshToken()).isEqualTo(expectedRefreshToken);

        then(tokenService).should(times(1)).saveRefreshToken(any(UUID.class), eq(expectedRefreshToken));
    }

    @DisplayName("Refresh Token 검증 중 만료 예외가 발생하면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenRefreshTokenIsExpiredDuringValidation() {
        //given
        String expiredToken = "expired.refresh.token";

        doThrow(new JwtAuthenticationException("Refresh token has expired."))
                .when(jwtProvider).validateRefreshToken(expiredToken);

        //when, then
        assertThatThrownBy(() -> authFacade.reissue(expiredToken))
                .isInstanceOf(JwtAuthenticationException.class)
                .hasMessage("Refresh token has expired.");

        then(tokenService).shouldHaveNoInteractions();
    }

    @DisplayName("클라이언트가 보낸 토큰과 Redis에 저장된 토큰이 일치하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowInvalidTokenException_whenRefreshTokenDoesNotMatchInStorage() {
        //given
        String clientRefreshToken = "client.refresh.token";
        String dbDifferentToken = "different.db.refresh.token";
        UUID userId = UUID.randomUUID();

        given(jwtProvider.getUserId(clientRefreshToken)).willReturn(userId.toString());
        given(tokenService.getRefreshToken(any(UUID.class))).willReturn(dbDifferentToken);

        //when, then
        assertThatThrownBy(() -> authFacade.reissue(clientRefreshToken))
                .isInstanceOf(JwtAuthenticationException.class)
                .hasMessage("Invalid refresh token.");

        then(jwtProvider).should(times(0)).generateAccessToken(anyString());
        then(tokenService).should(times(0)).saveRefreshToken(any(UUID.class), anyString());
    }

    @DisplayName("Redis에 유저의 토큰이 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowInvalidTokenException_whenRefreshTokenDoesNotExistInStorage() {
        //given
        String clientRefreshToken = "client.refresh.token";
        UUID userId = UUID.randomUUID();

        given(jwtProvider.getUserId(clientRefreshToken)).willReturn(userId.toString());
        given(tokenService.getRefreshToken(any(UUID.class))).willReturn(null);

        //when, then
        assertThatThrownBy(() -> authFacade.reissue(clientRefreshToken))
                .isInstanceOf(JwtAuthenticationException.class)
                .hasMessage("Invalid refresh token.");
    }

}
