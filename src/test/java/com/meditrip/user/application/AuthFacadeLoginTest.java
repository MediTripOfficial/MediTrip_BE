package com.meditrip.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.meditrip.common.jwt.JwtProvider;
import com.meditrip.user.application.dto.request.LoginApplicationRequest;
import com.meditrip.user.application.dto.response.TokenResponse;
import com.meditrip.user.domain.entity.User;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
class AuthFacadeLoginTest {

    @InjectMocks
    private AuthFacade authFacade;

    @Mock
    private UserService userService;

    @Mock
    private TokenService tokenService;

    @Mock
    private AuthService authService;

    @Mock
    private JwtProvider jwtProvider;

    @DisplayName("유저가 ACTIVE 상태이고, 비밀번호가 일치하면 로그인에 성공한다.")
    @Test
    void shouldSucceedLogin_whenUserIsActiveAndPasswordMatches() {
        //given
        String email = "test@test.com";
        String accessToken = "accessToken";
        String refreshToken = "refreshToken";
        UUID userId = UUID.randomUUID();
        User user = mock(User.class);

        given(user.getId()).willReturn(userId);
        given(user.getPassword()).willReturn("storedPassword");
        given(userService.findLoginUserByEmail(eq("로그인"), eq(email))).willReturn(user);
        given(jwtProvider.generateAccessToken(eq(userId.toString()))).willReturn(accessToken);
        given(jwtProvider.generateRefreshToken(eq(userId.toString()))).willReturn(refreshToken);

        LoginApplicationRequest request = LoginApplicationRequest.builder()
                .email(email)
                .password("password1234")
                .build();

        //when
        TokenResponse response = authFacade.login(request);

        //then
        assertThat(response.getAccessToken()).isEqualTo(accessToken);
        assertThat(response.getRefreshToken()).isEqualTo(refreshToken);

        verify(user, times(1)).validateStatusForLogin();
        verify(authService, times(1)).verifyPasswordForLogin(eq("password1234"), eq("storedPassword"));
        verify(tokenService, times(1)).saveRefreshToken(eq(userId), eq(refreshToken));
    }

    @DisplayName("가입되지 않은 이메일로 로그인 시도시 예외가 발생한다.")
    @Test
    void shouldThrowException_whenEmailDoesNotExist() {
        //given
        String email = "test@test.com";

        LoginApplicationRequest request = LoginApplicationRequest.builder()
                .email(email)
                .password("password1234")
                .build();

        given(userService.findLoginUserByEmail(eq("로그인"), eq(email)))
                .willThrow(new BadCredentialsException("Incorrect email or password."));

        //when, then
        assertThatThrownBy(() -> authFacade.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Incorrect email or password.");
    }

    @DisplayName("비밀번호가 일치하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenPasswordDoesNotMatch() {
        //given
        String email = "test@test.com";
        User user = mock(User.class);

        given(user.getPassword()).willReturn("storedPassword");
        given(userService.findLoginUserByEmail(eq("로그인"), eq(email))).willReturn(user);

        doThrow(new BadCredentialsException("Incorrect email or password."))
                .when(authService).verifyPasswordForLogin(eq("wrongPassword"), eq("storedPassword"));

        LoginApplicationRequest request = LoginApplicationRequest.builder()
                .email(email)
                .password("wrongPassword")
                .build();

        //when, then
        assertThatThrownBy(() -> authFacade.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Incorrect email or password.");
    }

    @DisplayName("유저의 상태가 DELETED라면 로그인에 실패한다.")
    @Test
    void shouldThrowException_whenUserStatusIsDeleted() {
        //given
        String email = "test@test.com";
        User user = mock(User.class);

        given(userService.findLoginUserByEmail(eq("로그인"), eq(email))).willReturn(user);
        doThrow(new BadCredentialsException("Incorrect email or password."))
                .when(user).validateStatusForLogin();

        LoginApplicationRequest request = LoginApplicationRequest.builder()
                .email(email)
                .password("password1234")
                .build();

        //when, then
        assertThatThrownBy(() -> authFacade.login(request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Incorrect email or password.");
    }

}
