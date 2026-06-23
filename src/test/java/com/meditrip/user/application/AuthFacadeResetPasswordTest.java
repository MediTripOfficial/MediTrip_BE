package com.meditrip.user.application;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

import com.meditrip.user.application.dto.request.ResetPasswordApplicationRequest;
import com.meditrip.user.domain.entity.User;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthFacadeResetPasswordTest {

    @InjectMocks
    private AuthFacade authFacade;

    @Mock
    private EmailAuthCodeStore emailAuthCodeStore;

    @Mock
    private UserService userService;

    @Mock
    private AuthService authService;

    private static final String EMAIL = "test@test.com";
    private static final String NEW_PASSWORD = "newPassword1234!";

    @DisplayName("비밀번호 초기화에 성공한다.")
    @Test
    void shouldSucceedResetPassword_successfully() {
        //given
        String verifiedToken = UUID.randomUUID().toString();
        User user = mock(User.class);

        ResetPasswordApplicationRequest request = ResetPasswordApplicationRequest.builder()
                .email(EMAIL)
                .password(NEW_PASSWORD)
                .verifiedToken(verifiedToken)
                .build();

        given(emailAuthCodeStore.findVerifiedTokenByEmail(EMAIL)).willReturn(verifiedToken);
        given(userService.findValidUserByEmail("비밀번호 초기화", EMAIL)).willReturn(user);

        //when
        authFacade.resetPassword(request);

        //then
        then(authService).should(times(1)).updatePassword(user, NEW_PASSWORD);
    }

    @DisplayName("이메일 인증 토큰이 없으면 비밀번호 초기화에 실패한다.")
    @Test
    void shouldFailResetPassword_whenVerifiedTokenNotFound() {
        //given
        ResetPasswordApplicationRequest request = ResetPasswordApplicationRequest.builder()
                .email(EMAIL)
                .password(NEW_PASSWORD)
                .verifiedToken(UUID.randomUUID().toString())
                .build();

        given(emailAuthCodeStore.findVerifiedTokenByEmail(EMAIL)).willReturn("");

        //when, then
        assertThatThrownBy(() -> authFacade.resetPassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email verification token is missing or has expired.");

        then(authService).should(never()).updatePassword(any(), any());
    }

    @DisplayName("이메일 인증 토큰이 일치하지 않으면 비밀번호 초기화에 실패한다.")
    @Test
    void shouldFailResetPassword_whenVerifiedTokenMismatch() {
        //given
        ResetPasswordApplicationRequest request = ResetPasswordApplicationRequest.builder()
                .email(EMAIL)
                .password(NEW_PASSWORD)
                .verifiedToken(UUID.randomUUID().toString())
                .build();

        given(emailAuthCodeStore.findVerifiedTokenByEmail(EMAIL))
                .willReturn(UUID.randomUUID().toString());

        //when, then
        assertThatThrownBy(() -> authFacade.resetPassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Email verification token is missing or has expired.");

        then(authService).should(never()).updatePassword(any(), any());
    }

}
