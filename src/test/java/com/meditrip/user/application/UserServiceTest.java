package com.meditrip.user.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.meditrip.common.domain.UserStatus;
import com.meditrip.user.domain.entity.User;
import com.meditrip.user.domain.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @InjectMocks
    private UserService userService;

    @Mock
    private UserRepository userRepository;

    private static final List<UserStatus> VALID_STATUSES = List.of(
            UserStatus.ACTIVE, UserStatus.GUEST
    );

    @DisplayName("유저 정보가 존재하면 유저를 찾을 수 있다.")
    @Test
    void shouldFindUser_whenUserExists() {
        //given
        String email = "test@test.com";

        User user = mock(User.class);
        given(userRepository.findByEmailAndStatusIn(email, VALID_STATUSES)).willReturn(Optional.of(user));

        //when, then
        assertThat(userService.findLoginUserByEmail("로그인", email)).isEqualTo(user);
    }

    @DisplayName("유저가 존재하지 않으면 예외가 발생한다.")
    @Test
    void shouldThrowException_whenUserDoesNotExist() {
        //given
        String email = "test@test.com";

        given(userRepository.findByEmailAndStatusIn(email, VALID_STATUSES)).willReturn(Optional.empty());

        //when, then
        assertThatThrownBy(()->userService.findLoginUserByEmail("로그인", email))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Incorrect email or password.");
    }

}
