package com.meditrip.user.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.BDDMockito.given;

import com.meditrip.user.domain.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServicePasswordTest {

    @InjectMocks
    private AuthService authService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @DisplayName("비밀번호가 일치하면 예외가 발생하지 않는다.")
    @Test
    void shouldNotThrowException_whenPasswordMatches() {
        //given
        String inputPassword = "asdfqwer1234$";
        String storedPassword = "asdfqwer1234$";

        given(passwordEncoder.matches(inputPassword, storedPassword)).willReturn(true);

        //when, then
        assertDoesNotThrow(() -> authService.verifyPasswordForLogin(inputPassword, storedPassword));
    }

    @DisplayName("비밀번호가 일치하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenPasswordDoesNotMatch() {
        //given
        String inputPassword = "asdfqwer1234$";
        String storedPassword = "passwordpassword13!";

        given(passwordEncoder.matches(inputPassword, storedPassword)).willReturn(false);

        //when, then
        assertThatThrownBy(() -> authService.verifyPasswordForLogin(inputPassword, storedPassword))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Incorrect email or password.");
    }

}
